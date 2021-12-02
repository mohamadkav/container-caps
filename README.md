# container-caps
Least-privilege capabilities for containers


## Prerequisites 
* Install Docker. I used the ONLY the step 4 "Install Docker" on this link: https://computingforgeeks.com/deploy-kubernetes-cluster-on-ubuntu-with-kubeadm/
* Install Java Development Toolkit (e.g., openJDK)

## Steps

1. Download the Docker image you want to try on. e.g., `docker pull gcc`
2. Run the image and make sure everything is fine with default configuration
3. Change line 9 of the code and refer it to a folder of your choice. This helps debugging the program further
4. Compile the `Main.java` code: `javac Main.java`
5. Run the program `java Main`
6. It asks you three questions. One is the image you want to run, second is optional port bindings (sometimes required for server apps), third is the flags you need to input to the `docker run` command. The default works in some cases but that's not the usual case. I had to run `gcc` with `-dit` params
7. The program will run and give you the least privilege capabilities at the end

## How it works
This program works by removing each default capability and checking the `docker log <container>` command for errors. If you see another error message (e.g., "fatal", "failed"), you can add it to line 10 of the Java code.

I have already run "alpine", "ubuntu", "centos", "gcc", "openjdk", "mysql", and "mongo" images.

