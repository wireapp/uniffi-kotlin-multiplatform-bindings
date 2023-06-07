/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

use std::cell::RefCell;
use std::sync::RwLock;

pub struct VoidCallbackProcessor {
    new_value: u64
}

pub trait VoidCallback: Send + Sync + std::fmt::Debug {
    fn call_back(&self, new_value: u64);
}

impl VoidCallbackProcessor {
    pub fn new(new_value: u64) -> Self {
        VoidCallbackProcessor {
            new_value
        }
    }

    pub fn process(&self, void_callback: Box<dyn VoidCallback>) {
        void_callback.call_back(self.new_value);
    }
}

pub trait VoidCallbackWithError: Send + Sync + std::fmt::Debug {
    fn call_back(&self, new_value: u64) -> Result<(), ComplexError>;
}

pub struct VoidCallbackWithErrorProcessor {
    callback: Box<dyn VoidCallbackWithError>
}

impl VoidCallbackWithErrorProcessor {
    pub fn new(callback: Box<dyn VoidCallbackWithError>) -> Self {
        VoidCallbackWithErrorProcessor {
            callback
        }
    }

    pub fn process(&self, new_value: u64) -> Result<(), ComplexError> {
        self.callback.call_back(new_value)
    }
}

trait ForeignGetters {
    fn get_bool(&self, v: bool, argument_two: bool) -> Result<bool, SimpleError>;
    fn get_string(&self, v: String, arg2: bool) -> Result<String, SimpleError>;
    fn get_option(&self, v: Option<String>, arg2: bool) -> Result<Option<String>, ComplexError>;
    fn get_list(&self, v: Vec<i32>, arg2: bool) -> Result<Vec<i32>, SimpleError>;
}

#[derive(Debug, thiserror::Error)]
pub enum SimpleError {
    #[error("BadArgument")]
    BadArgument,
    #[error("InternalTelephoneError")]
    UnexpectedError,
}

#[derive(Debug, thiserror::Error)]
pub enum ComplexError {
    #[error("ReallyBadArgument")]
    ReallyBadArgument { code: i32 },
    #[error("InternalTelephoneError")]
    UnexpectedErrorWithReason { reason: String },
}

impl From<uniffi::UnexpectedUniFFICallbackError> for SimpleError {
    fn from(_: uniffi::UnexpectedUniFFICallbackError) -> SimpleError {
        SimpleError::UnexpectedError
    }
}

impl From<uniffi::UnexpectedUniFFICallbackError> for ComplexError {
    fn from(e: uniffi::UnexpectedUniFFICallbackError) -> ComplexError {
        ComplexError::UnexpectedErrorWithReason { reason: e.reason }
    }
}

#[uniffi::export]
pub fn cthulu(a: u64, b: u64) -> u64 {
    a + b
}

pub fn meow(v: u64) -> u64 {
    v
}

#[uniffi::export]
pub async fn meow_async(v: u64) -> u64 {
    v
}

#[uniffi::export]
pub async fn async_error(v: u64) -> Result<u64, SimpleError> {
    if v == 42 {
        Ok(v)
    } else {
        Err(SimpleError::BadArgument)
    }
}

#[uniffi::export]
pub async fn async_unit(v: u64) {

}

#[uniffi::export]
pub async fn async_no_input_param() {

}

#[uniffi::export]
pub async fn do_nothing(v: u64) {}

#[uniffi::export]
pub async fn do_something(v: u64) -> u8 {
    42
}

#[uniffi::export]
pub async fn do_a_bit_more(v: u64) -> u16 {
    42
}

#[uniffi::export]
pub async fn do_a_lot_more(v: u64) -> u32 {
    42
}

#[uniffi::export]
pub async fn do_a_whole_lot_more(v: u64) -> u64 {
    v
}

#[derive(Debug, Clone)]
pub struct RustGetters;

pub struct Caller {
    inner: RwLock<u64>
}

impl Caller {
    pub fn new() -> Caller { Caller { inner: RwLock::new(41)} }
}

#[uniffi::export]
impl Caller {
    pub async fn call(&self) -> u64 {
        42
    }
    pub async fn interior_mutation(&self) {
        let mut mut_inner = self.inner.write().unwrap();
        *mut_inner = 42
    }
    fn get_inner(&self) -> u64 {
        let guard = self.inner.read().unwrap();
        guard.clone()
    }
}

impl RustGetters {
    pub fn new() -> Self {
        RustGetters
    }
    fn get_bool(
        &self,
        callback: Box<dyn ForeignGetters>,
        v: bool,
        argument_two: bool,
    ) -> Result<bool, SimpleError> {
        let ret = callback.get_bool(v, argument_two);
        ret
    }
    fn get_string(
        &self,
        callback: Box<dyn ForeignGetters>,
        v: String,
        arg2: bool,
    ) -> Result<String, SimpleError> {
        callback.get_string(v, arg2)
    }
    fn get_option(
        &self,
        callback: Box<dyn ForeignGetters>,
        v: Option<String>,
        arg2: bool,
    ) -> Result<Option<String>, ComplexError> {
        callback.get_option(v, arg2)
    }
    fn get_list(
        &self,
        callback: Box<dyn ForeignGetters>,
        v: Vec<i32>,
        arg2: bool,
    ) -> Result<Vec<i32>, SimpleError> {
        callback.get_list(v, arg2)
    }

    fn get_string_optional_callback(
        &self,
        callback: Option<Box<dyn ForeignGetters>>,
        v: String,
        arg2: bool,
    ) -> Result<Option<String>, SimpleError> {
        callback.map(|c| c.get_string(v, arg2)).transpose()
    }
}

impl Default for RustGetters {
    fn default() -> Self {
        Self::new()
    }
}

// Use `Send+Send` because we want to store the callback in an exposed
// `Send+Sync` object.
#[allow(clippy::wrong_self_convention)]
trait StoredForeignStringifier: Send + Sync + std::fmt::Debug {
    fn from_simple_type(&self, value: i32) -> String;
    fn from_complex_type(&self, values: Option<Vec<Option<f64>>>) -> String;
}

#[derive(Debug)]
pub struct RustStringifier {
    callback: Box<dyn StoredForeignStringifier>,
}

impl RustStringifier {
    fn new(callback: Box<dyn StoredForeignStringifier>) -> Self {
        RustStringifier { callback }
    }

    #[allow(clippy::wrong_self_convention)]
    fn from_simple_type(&self, value: i32) -> String {
        self.callback.from_simple_type(value)
    }
}

// include!(concat!(env!("OUT_DIR"), "/callbacks.uniffi.rs"));
uniffi::include_scaffolding!("callbacks");
uniffi_reexport_scaffolding!();
