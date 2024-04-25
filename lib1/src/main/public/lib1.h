#pragma once

#define FOO 42

// hide the symbol so transitive shared library don't include the symbols
__attribute__((visibility("hidden"))) int foo();