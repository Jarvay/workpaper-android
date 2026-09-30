use jni::JNIEnv;
use jni::objects::{JClass, JObject, JValue};
use jni::sys::{jboolean, jint, jstring};

use crate::Bitmap;

#[no_mangle]
pub extern "system" fn Java_jarvay_workpaper_rust_BitmapProcessor_testConnection<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
) -> jstring {
    match env.new_string("JNI Connection OK - Rust is working!") {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_jarvay_workpaper_rust_BitmapProcessor_passthrough<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
    java_bitmap: JObject<'a>,
) -> JObject<'a> {
    let local_ref = env.new_local_ref(java_bitmap).unwrap_or(JObject::null());
    local_ref
}

fn java_bitmap_to_rust<'a>(env: &mut JNIEnv<'a>, java_bitmap: JObject<'a>) -> Result<Bitmap, jni::errors::Error> {
    let width: jint = env.call_method(&java_bitmap, "getWidth", "()I", &[])?.i()?;
    let height: jint = env.call_method(&java_bitmap, "getHeight", "()I", &[])?.i()?;

    let pixels_len = (width * height) as i32;
    let pixels_array = env.new_int_array(pixels_len)?;

    env.call_method(
        &java_bitmap,
        "getPixels",
        "([IIIIIII)V",
        &[
            JValue::Object(&pixels_array),
            JValue::Int(0),
            JValue::Int(width),
            JValue::Int(0),
            JValue::Int(0),
            JValue::Int(width),
            JValue::Int(height),
        ],
    )?;

    let mut pixels_i32 = vec![0i32; pixels_len as usize];
    env.get_int_array_region(pixels_array, 0, &mut pixels_i32)?;

    let mut pixels_u8 = Vec::with_capacity((pixels_len as usize) * 4);
    for &pixel in pixels_i32.iter() {
        let a = ((pixel >> 24) & 0xFF) as u8;
        let r = ((pixel >> 16) & 0xFF) as u8;
        let g = ((pixel >> 8) & 0xFF) as u8;
        let b = (pixel & 0xFF) as u8;
        pixels_u8.push(r);
        pixels_u8.push(g);
        pixels_u8.push(b);
        pixels_u8.push(a);
    }

    Ok(Bitmap::new(width as u32, height as u32, pixels_u8))
}

fn rust_bitmap_to_java<'a>(env: &mut JNIEnv<'a>, bitmap: Bitmap) -> Result<JObject<'a>, jni::errors::Error> {
    let len = (bitmap.width * bitmap.height) as usize;
    let mut pixels_i32 = vec![0i32; len];

    for i in 0..len {
        let r = bitmap.pixels[i * 4];
        let g = bitmap.pixels[i * 4 + 1];
        let b = bitmap.pixels[i * 4 + 2];
        let a = bitmap.pixels[i * 4 + 3];
        pixels_i32[i] = ((a as i32) << 24) | ((r as i32) << 16) | ((g as i32) << 8) | (b as i32);
    }

    let pixels_array = env.new_int_array(len as i32)?;
    env.set_int_array_region(&pixels_array, 0, &pixels_i32)?;

    let config_class = env.find_class("android/graphics/Bitmap$Config")?;
    let config_obj = env.get_static_field(config_class, "ARGB_8888", "Landroid/graphics/Bitmap$Config;")?.l()?;

    let bitmap_class = env.find_class("android/graphics/Bitmap")?;
    let java_bitmap = env.call_static_method(
        bitmap_class,
        "createBitmap",
        "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;",
        &[
            JValue::Int(bitmap.width as jint),
            JValue::Int(bitmap.height as jint),
            JValue::Object(&config_obj),
        ],
    )?.l()?;

    env.call_method(
        &java_bitmap,
        "setPixels",
        "([IIIIIII)V",
        &[
            JValue::Object(&pixels_array),
            JValue::Int(0),
            JValue::Int(bitmap.width as jint),
            JValue::Int(0),
            JValue::Int(0),
            JValue::Int(bitmap.width as jint),
            JValue::Int(bitmap.height as jint),
        ],
    )?;

    Ok(java_bitmap)
}

#[no_mangle]
pub extern "system" fn Java_jarvay_workpaper_rust_BitmapProcessor_scaleFixedRatio<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
    java_bitmap: JObject<'a>,
    target_width: jint,
    target_height: jint,
    use_min: jboolean,
) -> JObject<'a> {
    let mut env = env;
    let rust_bitmap = match java_bitmap_to_rust(&mut env, java_bitmap) {
        Ok(bitmap) => bitmap,
        Err(_) => return JObject::null(),
    };

    let result_bitmap = rust_bitmap.scale_fixed_ratio(target_width as u32, target_height as u32, use_min != 0);

    match rust_bitmap_to_java(&mut env, result_bitmap) {
        Ok(java_bitmap) => java_bitmap,
        Err(_) => JObject::null(),
    }
}

#[no_mangle]
pub extern "system" fn Java_jarvay_workpaper_rust_BitmapProcessor_centerCrop<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
    java_bitmap: JObject<'a>,
    target_width: jint,
    target_height: jint,
) -> JObject<'a> {
    let mut env = env;
    let rust_bitmap = match java_bitmap_to_rust(&mut env, java_bitmap) {
        Ok(bitmap) => bitmap,
        Err(_) => return JObject::null(),
    };

    let result_bitmap = rust_bitmap.center_crop(target_width as u32, target_height as u32);

    match rust_bitmap_to_java(&mut env, result_bitmap) {
        Ok(java_bitmap) => java_bitmap,
        Err(_) => JObject::null(),
    }
}

#[no_mangle]
pub extern "system" fn Java_jarvay_workpaper_rust_BitmapProcessor_getInfo<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
    java_bitmap: JObject<'a>,
) -> jstring {
    let mut env = env;
    let rust_bitmap = match java_bitmap_to_rust(&mut env, java_bitmap) {
        Ok(bitmap) => bitmap,
        Err(_) => {
            return env.new_string("").unwrap().into_raw();
        }
    };

    let info = rust_bitmap.info();

    match env.new_string(info) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

#[no_mangle]
pub extern "system" fn Java_jarvay_workpaper_rust_BitmapProcessor_blur<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
    java_bitmap: JObject<'a>,
    radius: jint,
) -> JObject<'a> {
    let mut env = env;
    let rust_bitmap = match java_bitmap_to_rust(&mut env, java_bitmap) {
        Ok(bitmap) => bitmap,
        Err(_) => return JObject::null(),
    };

    let result_bitmap = rust_bitmap.blur(radius as u8);

    match rust_bitmap_to_java(&mut env, result_bitmap) {
        Ok(java_bitmap) => java_bitmap,
        Err(_) => JObject::null(),
    }
}

#[no_mangle]
pub extern "system" fn Java_jarvay_workpaper_rust_BitmapProcessor_noise<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
    java_bitmap: JObject<'a>,
    percent: jint,
) -> JObject<'a> {
    let mut env = env;
    let rust_bitmap = match java_bitmap_to_rust(&mut env, java_bitmap) {
        Ok(bitmap) => bitmap,
        Err(_) => return JObject::null(),
    };

    let result_bitmap = rust_bitmap.noise(percent as u8);

    match rust_bitmap_to_java(&mut env, result_bitmap) {
        Ok(java_bitmap) => java_bitmap,
        Err(_) => JObject::null(),
    }
}

#[no_mangle]
pub extern "system" fn Java_jarvay_workpaper_rust_BitmapProcessor_effect<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
    java_bitmap: JObject<'a>,
    brightness: jint,
    contrast: jint,
    saturation: jint,
) -> JObject<'a> {
    let mut env = env;
    let rust_bitmap = match java_bitmap_to_rust(&mut env, java_bitmap) {
        Ok(bitmap) => bitmap,
        Err(_) => return JObject::null(),
    };

    let result_bitmap = rust_bitmap.effect(brightness as i32, contrast as i32, saturation as i32);

    match rust_bitmap_to_java(&mut env, result_bitmap) {
        Ok(java_bitmap) => java_bitmap,
        Err(_) => JObject::null(),
    }
}

#[no_mangle]
pub extern "system" fn Java_jarvay_workpaper_rust_BitmapProcessor_setAlpha<'a>(
    env: JNIEnv<'a>,
    _class: JClass<'a>,
    java_bitmap: JObject<'a>,
    alpha: jint,
) -> JObject<'a> {
    let mut env = env;
    let rust_bitmap = match java_bitmap_to_rust(&mut env, java_bitmap) {
        Ok(bitmap) => bitmap,
        Err(_) => return JObject::null(),
    };

    let result_bitmap = rust_bitmap.set_alpha(alpha as u8);

    match rust_bitmap_to_java(&mut env, result_bitmap) {
        Ok(java_bitmap) => java_bitmap,
        Err(_) => JObject::null(),
    }
}