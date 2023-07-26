use uniffi_bindgen::backend::{CodeOracle, CodeType};

pub struct ForeignExecutorCodeType;

impl CodeType for ForeignExecutorCodeType {
    fn type_label(&self, _oracle: &dyn CodeOracle) -> String {
        // TODO itesign check this also works in Kotlin/Native
        // Kotlin uses a CoroutineScope for ForeignExecutor
        "CoroutineScope".into()
    }

    fn canonical_name(&self, _oracle: &dyn CodeOracle) -> String {
        "ForeignExecutor".into()
    }

    fn initialization_fn(&self, _oracle: &dyn CodeOracle) -> Option<String> {
        // FfiConverterForeignExecutor is a Kotlin object generated from a template
        // register calls lib.uniffi_foreign_executor_callback_set(UniFfiForeignExecutorCallback) where
        // object UniFfiForeignExecutorCallback : com.sun.jna.Callback
        // but that will not work in Kotlin/Native since we do not have access to JNA
        Some("FfiConverterForeignExecutor.register".into())
    }
}
