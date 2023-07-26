use camino::Utf8Path;
use uniffi_kotlin_multiplatform::KotlinBindingGenerator;

fn main() {
    uniffi_build::generate_scaffolding("./src/coverall.udl").unwrap();
}
