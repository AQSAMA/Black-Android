#pragma once
// included before every host source by CMake: the SDK's kernel/memory.cpp calls ftruncate, which glibc's headers bring
// along and bionic's do not
#include <unistd.h>
