use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use md5::{Digest, Md5};
use serde::{Deserialize, Serialize};
use std::fs;
use std::io::Write;
use std::path::Path;

#[derive(Deserialize, Serialize)]
struct WallpaperRaw {
    name: String,
    thumbnail_url: String,
    full_url: String,
}

fn throw_exception(env: &mut JNIEnv, class: &str, msg: &str) {
    let _ = env.throw_new(class, msg);
}

#[no_mangle]
pub extern "system" fn Java_com_shinkai_wallpapers_NativeLib_fetchWallpapersNative(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
) -> jstring {
    let url_str: String = env.get_string(&url).unwrap().into();

    let body = match ureq::get(&url_str).call() {
        Ok(resp) => resp.into_string().unwrap_or_default(),
        Err(_) => {
            throw_exception(&mut env, "java/io/IOException", "Failed to fetch wallpapers from network");
            return std::ptr::null_mut();
        }
    };

    let raw: Vec<WallpaperRaw> = match serde_json::from_str(&body) {
        Ok(v) => v,
        Err(e) => {
            let msg = format!("JSON parse error: {e}");
            throw_exception(&mut env, "org/json/JSONException", &msg);
            return std::ptr::null_mut();
        }
    };

    let json_out = serde_json::to_string(&raw).unwrap_or_else(|_| "[]".into());
    let output = env.new_string(&json_out).unwrap();
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_shinkai_wallpapers_NativeLib_downloadImageNative(
    mut env: JNIEnv,
    _class: JClass,
    url: JString,
    cache_dir: JString,
) -> jstring {
    let url_str: String = env.get_string(&url).unwrap().into();
    let cache_str: String = env.get_string(&cache_dir).unwrap().into();

    let file_name = hash_key(&url_str);
    let cache_path = Path::new(&cache_str).join("image_cache");
    let _ = fs::create_dir_all(&cache_path);

    let file_path = cache_path.join(&file_name);

    if file_path.exists() {
        let output = env.new_string(file_path.to_str().unwrap()).unwrap();
        return output.into_raw();
    }

    let tmp_path = cache_path.join(format!("{file_name}.tmp"));

    let result = (|| -> Result<(), Box<dyn std::error::Error>> {
        let resp = ureq::get(&url_str)
            .timeout(std::time::Duration::from_secs(10))
            .call()?;

        let mut reader = resp.into_reader();
        let mut file = fs::File::create(&tmp_path)?;
        std::io::copy(&mut reader, &mut file)?;
        file.flush()?;
        drop(file);

        fs::rename(&tmp_path, &file_path)?;
        Ok(())
    })();

    match result {
        Ok(()) => {
            let output = env.new_string(file_path.to_str().unwrap()).unwrap();
            output.into_raw()
        }
        Err(e) => {
            let _ = fs::remove_file(&tmp_path);
            let msg = format!("Image download failed: {e}");
            throw_exception(&mut env, "java/io/IOException", &msg);
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_shinkai_wallpapers_NativeLib_hashKeyNative(
    mut env: JNIEnv,
    _class: JClass,
    key: JString,
) -> jstring {
    let key_str: String = env.get_string(&key).unwrap().into();
    let result = hash_key(&key_str);
    let output = env.new_string(&result).unwrap();
    output.into_raw()
}

fn hash_key(key: &str) -> String {
    let mut hasher = Md5::new();
    hasher.update(key.as_bytes());
    let result = hasher.finalize();
    result.iter().map(|b| format!("{b:02x}")).collect()
}
