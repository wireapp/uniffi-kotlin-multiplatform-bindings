use std::fs;
use std::process::Command;

use camino::Utf8Path;

use uniffi_kotlin_multiplatform::KotlinBindingGenerator;

fn main() {
    Command::new("cargo").args(["build", "--manifest-path", "./crate_one/Cargo.toml"]).output().unwrap();
    Command::new("cargo").args(["build", "--manifest-path", "./crate_two/Cargo.toml"]).output().unwrap();
    fs::copy("crate_one/target/debug/libcrate_one.a", "target/debug/libcrate_one.a").unwrap();
    fs::copy("crate_two/target/debug/libcrate_two.a", "target/debug/libcrate_two.a").unwrap();
    let tests = vec!["external_types"];
    let out_dir = Utf8Path::new("target/bindings");
    for test in tests.iter() {
        let udl_file_path = format!("./src/{}.udl", test);
        let udl_file = Utf8Path::new(&udl_file_path);
        uniffi::generate_scaffolding(udl_file).unwrap();
        uniffi_bindgen::generate_external_bindings(
            KotlinBindingGenerator {},
            udl_file,
            None::<&Utf8Path>,
            Some(out_dir),
        ).unwrap();
    }
}
