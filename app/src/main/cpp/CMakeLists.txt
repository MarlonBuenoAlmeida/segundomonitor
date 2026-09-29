cmake_minimum_required(VERSION 3.10.2)

project("tabletdecoder")

add_library(
        tabletdecoder
        SHARED
        native-streamer.cpp)

find_library(
        log-lib
        log)

find_library(
        android-lib
        android)

target_link_libraries(
        tabletdecoder
        ${log-lib}
        ${android-lib})