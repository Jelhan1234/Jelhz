cmake_minimum_required(VERSION 3.10.2)
project("fakemenu")

add_library(fakemenu SHARED native-lib.cpp)
find_library(log-lib log)
target_link_libraries(fakemenu ${log-lib})
