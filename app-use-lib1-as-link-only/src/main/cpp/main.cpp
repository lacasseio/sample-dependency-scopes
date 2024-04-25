int foo(); // redeclare as lib1.h is not pulled

int main() {
    return foo(); // expected to succeed as the static library containing the symbol is pulled
}