cmake_minimum_required(VERSION 2.8.3)
project(android_apps)

find_package(catkin REQUIRED rosjava_tools)

catkin_android_setup(assembleRelease publishToMavenLocal)

catkin_package()

