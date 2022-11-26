use camino::Utf8Path;
use uniffi_kotlin_multiplatform::KotlinBindingGenerator;

fn main() {
    let tests = vec!["coverall"];
    let out_dir = Utf8Path::new("target/bindings");
    for test in tests.iter() {
        let udl_file_path = format!("./src/{}.udl", test);
        let udl_file = Utf8Path::new(&udl_file_path);
        uniffi_build::generate_scaffolding(udl_file).unwrap();
        uniffi_bindgen::generate_external_bindings(
            KotlinBindingGenerator {},
            udl_file,
            None::<&Utf8Path>,
            Some(out_dir),
        ).unwrap();
    }
}
