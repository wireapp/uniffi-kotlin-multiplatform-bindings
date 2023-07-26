use std::fs;
use std::process::Command;

use camino::Utf8Path;

use uniffi_kotlin_multiplatform::KotlinBindingGenerator;

fn main() {
    Command::new("cargo").args(["build", "--manifest-path", "./crate_one/Cargo.toml"]).output().unwrap();
    Command::new("cargo").args(["build", "--manifest-path", "./crate_two/Cargo.toml"]).output().unwrap();
    fs::copy("crate_one/target/debug/libcrate_one.a", "target/debug/libcrate_one.a").unwrap();
    fs::copy("crate_two/target/debug/libcrate_two.a", "target/debug/libcrate_two.a").unwrap();
    uniffi_build::generate_scaffolding("./src/external_types.udl").unwrap();
}
