#include "lib1.h"
#include "lib2.h"

int far() {
    return foo() + bar();
}