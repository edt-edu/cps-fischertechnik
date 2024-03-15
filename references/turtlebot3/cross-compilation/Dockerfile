FROM ros:humble
RUN apt update
RUN apt install -y python3-argcomplete python3-colcon-common-extensions libboost-system-dev build-essential
RUN apt install -y ros-humble-hls-lfcd-lds-driver
RUN apt install -y ros-humble-turtlebot3-msgs
RUN apt install -y ros-humble-dynamixel-sdk
RUN apt install -y libudev-dev
RUN mkdir -p /home/pi/turtlebot3_ws/src && cd /home/pi/turtlebot3_ws/src \
    && git clone -b humble-devel https://github.com/ROBOTIS-GIT/turtlebot3.git \
    && git clone -b ros2-devel https://github.com/ROBOTIS-GIT/ld08_driver.git
RUN cd /home/pi/turtlebot3_ws/src/turtlebot3 && rm -r turtlebot3_cartographer turtlebot3_navigation2
RUN cd /home/pi/turtlebot3_ws/ \
    && . /opt/ros/humble/setup.sh \
    && colcon build --symlink-install