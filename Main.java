import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    
    private static String workingDirectory="/home/mohammad/Desktop/dockerScripts/";
    private static String[] errorMessages={"error"};



    private static String[] defaultCaps={"CAP_AUDIT_WRITE","CAP_DAC_OVERRIDE","CAP_FOWNER","CAP_FSETID","CAP_KILL","CAP_MKNOD","CAP_NET_BIND_SERVICE","CAP_NET_RAW","CAP_SETFCAP","CAP_SETGID","CAP_SETPCAP","CAP_SETUID","CAP_SYS_CHROOT","CAP_CHOWN"};
    public static void main(String[] args) throws Exception{
        //Read image name and params
        Scanner input = new Scanner(System.in);
        System.out.println("Enter image name and tag (example: mysql:yi)");
        String imageName=input.nextLine();
        if(imageName.trim().isEmpty()) //default for MySQL
            imageName="mysql:yi";
        System.out.println("Enter additional parameters (e.g., for a port bind, \"-p 23306:3306\")");
        String additionalParams=input.nextLine();
        if(additionalParams.trim().isEmpty()) //default for MySQL
            additionalParams="-p 23306:3306";
        String parameters="--rm -d";
        System.out.println("Input parameters (default: --rm -d):");
        String paramInput=input.nextLine();
        if(!paramInput.trim().isEmpty())
            parameters=paramInput;
        //List of caps that can be removed (default empty)
        List<String> canBeRemoved=new ArrayList<>();

        for(String cap:defaultCaps) {
            //Start the image
            //Original command:
    //        String command = "docker run --cap-drop=CAP_FOWNER --cap-drop=CAP_FSETID --cap-drop=CAP_KILL --cap-drop=CAP_NET_BIND_SERVICE --cap-drop=CAP_NET_RAW --cap-drop=CAP_SETFCAP --cap-drop=CAP_SETPCAP  --cap-drop=CAP_SYS_CHROOT --cap-drop=CAP_CHOWN --rm -d " + additionalParams + " " + imageName;
            String command = "docker run --cap-drop="+cap+" "+parameters+" "+ additionalParams + " " + imageName;

            System.out.println("Running command: " + command);
            PrintWriter output = new PrintWriter(new FileOutputStream(workingDirectory + "startDocker.sh", false));
            output.println(command);
            output.close();

            ProcessBuilder builder = new ProcessBuilder("sh", workingDirectory + "startDocker.sh");
            builder.redirectOutput(new File(workingDirectory + "startDockerOut.txt"));
            builder.redirectError(new File(workingDirectory + "startDockerOut.txt"));
            Process p = builder.start();

            System.out.println("\nStarted. Waiting for 2secs for the docker to startup...");
            Thread.sleep(2000);

            //Run command to get the imageID
            command = "docker ps";
            System.out.println("\nGetting image id: " + command);
            output = new PrintWriter(new FileOutputStream(workingDirectory + "getImageId.sh", false));
            output.println(command);
            output.close();

            builder = new ProcessBuilder("sh", workingDirectory + "getImageId.sh");
            builder.redirectErrorStream(true);
            builder.redirectOutput(new File(workingDirectory + "getImageIdOut.txt"));
            p = builder.start();
            Thread.sleep(1000);


            //Get imageID from the file:
            input = new Scanner(new File(workingDirectory + "getImageIdOut.txt"));
            input.nextLine();
            String imageId = input.nextLine().split("\\s+")[0];
            System.out.println("Got image id: " + imageId);

            if (input.hasNext())
                System.err.println("WARNING: MORE THAN ONE IMAGE RUNNING");


            //Get logs from the Docker image
            command = "docker logs " + imageId;
            System.out.println("Getting image logs: " + command);
            output = new PrintWriter(new FileOutputStream(workingDirectory + "getImageLog.sh", false));
            output.println(command);
            output.close();

            builder = new ProcessBuilder("sh", workingDirectory + "getImageLog.sh");
            builder.redirectErrorStream(true);
            builder.redirectOutput(new File(workingDirectory + "imageLog.txt"));
            p = builder.start();
            Thread.sleep(1000);

            //Parse the image log for errors
            input = new Scanner(new File(workingDirectory + "imageLog.txt"));
            boolean error=false;
            while (input.hasNext()) {
                String raw = input.nextLine();
                for (String errorMessage : errorMessages) {
                    if (raw.toLowerCase().contains(errorMessage)) {
                        System.out.println("Capability " + cap + " cannot be removed");
                        error = true;
                        break;
                    }
                }
                if(error)
                    break;
            }
            if(!error) {
                canBeRemoved.add(cap);
                System.out.println("OK");
            }

            //Stop the docker image for next iteration
            command = "docker stop " + imageId;
            System.out.println("\nStopping docker image: " + command);
            output = new PrintWriter(new FileOutputStream(workingDirectory + "stopDockerImage.sh", false));
            output.println(command);
            output.close();

            builder = new ProcessBuilder("sh", workingDirectory + "stopDockerImage.sh");
            builder.redirectErrorStream(true);
            builder.redirectOutput(new File(workingDirectory + "stopImageLog.txt"));
            p = builder.start();
            Thread.sleep(15000); //Takes long to stop the image
            System.out.println("Image stopped. Starting the next iteration\n");
        }
        System.out.println("CAPABILITIES THAT CAN BE REMOVED:");
        for(String cap:canBeRemoved)
            System.out.println(cap);

        System.out.println("\n\n Now you can test your workload by running this command:");
        String command = "docker run ";//--cap-drop="+cap+" --rm -d " + additionalParams + " " + imageName;
        for(String cap:canBeRemoved)
            command+="--cap-drop="+cap+" ";
        command+="--rm -d " + additionalParams + " " + imageName;
        System.out.println(command);

    }
}
