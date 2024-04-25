#include "lib1.h" // expected to succeeds thanks to compileOnly

int main() {
    return FOO + foo(); // expected to fail link step because only the API is pulled
}