# Cross-compiling the turtlebot3 software
Compiling the turtlebot3 software on the pi is slow and sometimes fails without good reasons.
Compiling on a real computer using docker is much faster.

This guide replaces Step 13 of https://emanual.robotis.com/docs/en/platform/turtlebot3/sbc_setup/#sbc-setup.

Instructions to cross compile using docker:
1. Check that docker is correctly installed: Check by calling `docker run hello-world`, which should print `Hello from Docker!...`
    - Otherwise: Install Docker. On Linux also execute the post install steps
2. Clone this repository
3. Change dir to this folder: `cd references/turtlebot3/cross-compilation`
4. Build the Dockerfile in this dir WITH THE PLATFROM `arm64` which is used by the turtlebot3: `docker build --platform linux/arm64 -t turtlebot3-builder .`
5. Prepare the modules(any adjustments to the modules can be done before this step)
```bash
mkdir turtlebot3_ws && mkdir turtlebot3_ws/src
cd turtlebot3_ws/src
git clone -b humble-devel https://github.com/ROBOTIS-GIT/turtlebot3.git
git clone -b ros2-devel https://github.com/ROBOTIS-GIT/ld08_driver.git
git clone -b humble-devel https://github.com/ROBOTIS-GIT/turtlebot3_manipulation.git
cd turtlebot3
rm -r turtlebot3_cartographer
rm -r turtlebot3_navigation2
cd ..
cd ..
cd ..
```
6. Build the modules with docker
- The ros workspace in the container and the ros workspace on the turtlebots PI MUST have the same abosulte path!
```
docker run --platform linux/arm64 -it -v "$(pwd)/turtlebot3_ws:/home/pi/turtlebot3_ws" turtlebot3-builder bash
# now in the container
cd /home/pi/turtlebot3_ws/
colcon build --symlink-install
# After a few minutes colcon should be done and print something like
# Summary: 7 packages finished
exit
```

7. Copy the entire `turtlebot3_ws` dir to the raspberry
  - Either using ssh or using an sd card reader
  - The ros workspace in the container and the ros workspace on the turtlebots PI MUST have the same abosulte path!
    - If you copy pasted the commands, it is `/home/pi/turtlebot3_ws/`
8. On the RaspberryPI Adapt your `.bashrc` to load the compiled modules and load them in the current session
  - `echo 'source ~/turtlebot3_ws/install/setup.bash' >> ~/.bashrc`
  - `source ~/.bashrc`

The modules should now be usable.
Continue with https://emanual.robotis.com/docs/en/platform/turtlebot3/sbc_setup/#sbc-setup Step 14. 
