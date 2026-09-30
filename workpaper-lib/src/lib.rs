//! # workpaper-lib
//!
//! A Rust library for image processing operations, inspired by Android's Bitmap class.
//!
//! ## Example
//!
//! ```rust
//! use workpaper_lib::Bitmap;
//! use image::RgbaImage;
//!
//! // Create a sample image
//! let img = RgbaImage::new(100, 100);
//! let bitmap = Bitmap::from_rgba_image(&img);
//!
//! // Scale the image
//! let scaled = bitmap.scale_fixed_ratio(50, 50, true);
//!
//! // Crop the image
//! let cropped = bitmap.center_crop(80, 80);
//!
//! // Apply effects
//! let noisy = bitmap.noise(50);
//! let blurred = bitmap.blur(5);
//! let adjusted = bitmap.effect(60, 70, 80); // brightness, contrast, saturation
//! let transparent = bitmap.set_alpha(128);
//!
//! // Get image info
//! println!("{}", bitmap.info());
//! ```

use image::{ImageBuffer, Rgba, RgbaImage};
use std::f32;
use rand_distr::{Distribution, Normal};

/// Represents an image with width, height and pixel data
pub struct Bitmap {
    pub width: u32,
    pub height: u32,
    pub pixels: Vec<u8>, // RGBA format: [r, g, b, a, r, g, b, a, ...]
}

// Include JNI interface if building for Android
#[cfg(target_os = "android")]
mod jni_interface;

impl Bitmap {
    /// Creates a new Bitmap from width, height and pixel data
    pub fn new(width: u32, height: u32, pixels: Vec<u8>) -> Self {
        let expected_size = (width * height * 4) as usize;
        let actual_size = pixels.len();

        if actual_size != expected_size {
            // If sizes don't match, create a blank bitmap of the requested dimensions
            let blank_pixels = vec![0u8; expected_size];
            return Bitmap { width, height, pixels: blank_pixels };
        }

        Bitmap { width, height, pixels }
    }

    /// Creates a Bitmap from an RgbaImage
    pub fn from_rgba_image(img: &RgbaImage) -> Self {
        let (width, height) = img.dimensions();

        if width == 0 || height == 0 {
            return Bitmap { width, height, pixels: vec![] };
        }

        let pixels: Vec<u8> = img.as_raw().clone();
        Bitmap { width, height, pixels }
    }

    /// Converts the Bitmap to an RgbaImage
    pub fn to_rgba_image(&self) -> RgbaImage {
        if self.width == 0 || self.height == 0 || self.pixels.is_empty() {
            return RgbaImage::new(0, 0);
        }

        ImageBuffer::from_fn(self.width, self.height, |x, y| {
            let idx = ((y * self.width + x) * 4) as usize;
            if idx + 3 < self.pixels.len() {
                Rgba([
                    self.pixels[idx],
                    self.pixels[idx + 1],
                    self.pixels[idx + 2],
                    self.pixels[idx + 3],
                ])
            } else {
                Rgba([0, 0, 0, 0]) // Return transparent black if index out of bounds
            }
        })
    }

    /// Scales the bitmap with fixed ratio
    pub fn scale_fixed_ratio(&self, target_width: u32, target_height: u32, use_min: bool) -> Bitmap {
        let scale_width = target_width as f32 / self.width as f32;
        let scale_height = target_height as f32 / self.height as f32;

        let scale_ratio = if use_min {
            scale_width.min(scale_height)
        } else {
            scale_width.max(scale_height)
        };

        let new_width = (self.width as f32 * scale_ratio) as u32;
        let new_height = (self.height as f32 * scale_ratio) as u32;

        self.resize(new_width, new_height)
    }

    /// Center crops the bitmap to target dimensions
    pub fn center_crop(&self, target_width: u32, target_height: u32) -> Bitmap {
        if target_width >= self.width && target_height >= self.height {
            return self.clone();
        }

        let src_rate = self.width as f32 / self.height as f32;
        let des_rate = target_width as f32 / target_height as f32;

        let mut dx = 0;
        let mut dy = 0;

        if (src_rate - des_rate).abs() < f32::EPSILON {
            // Rates are equal
        } else if src_rate > des_rate {
            dx = ((self.width - target_width) / 2) as i32;
        } else {
            dy = ((self.height - target_height) / 2) as i32;
        }

        self.crop(dx.max(0) as u32, dy.max(0) as u32, target_width, target_height)
    }

    /// Returns string representation of bitmap dimensions
    pub fn info(&self) -> String {
        format!("width: {}, height: {}", self.width, self.height)
    }

    /// Applies blur effect to the bitmap
    pub fn blur(&self, radius: u8) -> Bitmap {
        let radius = radius.min(25) as usize;
        if radius == 0 {
            return self.clone();
        }
        
        let width = self.width as usize;
        let height = self.height as usize;
        let mut pixels = self.pixels.clone();
        
        Self::box_blur_horizontal(&mut pixels, width, height, radius);
        Self::box_blur_vertical(&mut pixels, width, height, radius);
        
        Self::box_blur_horizontal(&mut pixels, width, height, radius);
        Self::box_blur_vertical(&mut pixels, width, height, radius);

        Bitmap {
            width: self.width,
            height: self.height,
            pixels,
        }
    }
    
    fn box_blur_horizontal(pixels: &mut [u8], width: usize, height: usize, radius: usize) {
        let channels = 4;
        let mut temp = vec![0f32; width * channels];
        
        for y in 0..height {
            let row_start = y * width * channels;
            
            for c in 0..channels {
                let mut sum = 0f32;
                let mut count = 0;
                
                for x in 0..=radius {
                    sum += pixels[row_start + x * channels + c] as f32;
                    count += 1;
                }
                
                temp[c] = sum / count as f32;
                
                for x in 1..width {
                    let left = x - radius - 1;
                    let right = x + radius;
                    
                    if left >= 0 {
                        sum -= pixels[row_start + left * channels + c] as f32;
                        count -= 1;
                    }
                    if right < width {
                        sum += pixels[row_start + right * channels + c] as f32;
                        count += 1;
                    }
                    
                    temp[x * channels + c] = sum / count as f32;
                }
            }
            
            for x in 0..width {
                let src = x * channels;
                let dst = row_start + x * channels;
                pixels[dst] = temp[src].clamp(0.0, 255.0) as u8;
                pixels[dst + 1] = temp[src + 1].clamp(0.0, 255.0) as u8;
                pixels[dst + 2] = temp[src + 2].clamp(0.0, 255.0) as u8;
                pixels[dst + 3] = temp[src + 3].clamp(0.0, 255.0) as u8;
            }
        }
    }
    
    fn box_blur_vertical(pixels: &mut [u8], width: usize, height: usize, radius: usize) {
        let channels = 4;
        let mut temp = vec![0f32; height * channels];
        
        for x in 0..width {
            for c in 0..channels {
                let mut sum = 0f32;
                let mut count = 0;
                
                for y in 0..=radius {
                    sum += pixels[y * width * channels + x * channels + c] as f32;
                    count += 1;
                }
                
                temp[c] = sum / count as f32;
                
                for y in 1..height {
                    let top = y - radius - 1;
                    let bottom = y + radius;
                    
                    if top >= 0 {
                        sum -= pixels[top * width * channels + x * channels + c] as f32;
                        count -= 1;
                    }
                    if bottom < height {
                        sum += pixels[bottom * width * channels + x * channels + c] as f32;
                        count += 1;
                    }
                    
                    temp[y * channels + c] = sum / count as f32;
                }
            }
            
            for y in 0..height {
                let src = y * channels;
                let dst = y * width * channels + x * channels;
                pixels[dst] = temp[src].clamp(0.0, 255.0) as u8;
                pixels[dst + 1] = temp[src + 1].clamp(0.0, 255.0) as u8;
                pixels[dst + 2] = temp[src + 2].clamp(0.0, 255.0) as u8;
                pixels[dst + 3] = temp[src + 3].clamp(0.0, 255.0) as u8;
            }
        }
    }

    /// Adds noise to the bitmap (Photoshop-style)
    /// Default: Monochrome noise with Gaussian distribution
    pub fn noise(&self, percent: u8) -> Bitmap {
        let percent = percent.min(100);
        let intensity = (percent as f32 / 100.0) * 255.0;
        
        let mut rng = rand::thread_rng();
        let normal = Normal::new(0.0, intensity / 3.0).unwrap();
        
        let mut new_pixels = self.pixels.clone();

        for i in (0..self.pixels.len()).step_by(4) {
            let r = self.pixels[i] as f32;
            let g = self.pixels[i + 1] as f32;
            let b = self.pixels[i + 2] as f32;

            let noise = normal.sample(&mut rng);

            new_pixels[i] = (r + noise).clamp(0.0, 255.0) as u8;
            new_pixels[i + 1] = (g + noise).clamp(0.0, 255.0) as u8;
            new_pixels[i + 2] = (b + noise).clamp(0.0, 255.0) as u8;
        }

        Bitmap {
            width: self.width,
            height: self.height,
            pixels: new_pixels,
        }
    }

    /// Applies brightness, contrast, and saturation effects
    /// Parameters: brightness (-50 to 50), contrast (-50 to 50), saturation (-50 to 50)
    pub fn effect(&self, brightness: i32, contrast: i32, saturation: i32) -> Bitmap {
        let lum = brightness as f32 * 2.0 * 0.3 * 255.0 * 0.01;
        let scale = (contrast + 100) as f32 / 100.0;
        let offset = 0.5 * (1.0 - scale);
        let sat = saturation as f32 / 50.0 * 0.3 + 1.0;

        let mut new_pixels = self.pixels.clone();

        for i in (0..self.pixels.len()).step_by(4) {
            let mut r = self.pixels[i] as f32;
            let mut g = self.pixels[i + 1] as f32;
            let mut b = self.pixels[i + 2] as f32;
            let _a = self.pixels[i + 3];

            r = r * scale + lum + offset * 255.0;
            g = g * scale + lum + offset * 255.0;
            b = b * scale + lum + offset * 255.0;

            let gray = r * 0.213 + g * 0.715 + b * 0.072;
            r = gray + (r - gray) * sat;
            g = gray + (g - gray) * sat;
            b = gray + (b - gray) * sat;

            new_pixels[i] = r.clamp(0.0, 255.0) as u8;
            new_pixels[i + 1] = g.clamp(0.0, 255.0) as u8;
            new_pixels[i + 2] = b.clamp(0.0, 255.0) as u8;
        }

        Bitmap {
            width: self.width,
            height: self.height,
            pixels: new_pixels,
        }
    }

    /// Sets alpha value for the entire bitmap
    pub fn set_alpha(&self, alpha: u8) -> Bitmap {
        let mut new_pixels = self.pixels.clone();

        for i in (3..new_pixels.len()).step_by(4) {
            new_pixels[i] = alpha;
        }

        Bitmap {
            width: self.width,
            height: self.height,
            pixels: new_pixels,
        }
    }

    /// Resizes the bitmap to new dimensions
    fn resize(&self, new_width: u32, new_height: u32) -> Bitmap {
        let img = self.to_rgba_image();
        let resized_img = image::imageops::resize(&img, new_width, new_height, image::imageops::Lanczos3);
        Bitmap::from_rgba_image(&resized_img)
    }

    /// Crops the bitmap to specified rectangle
    fn crop(&self, x: u32, y: u32, width: u32, height: u32) -> Bitmap {
        let img = self.to_rgba_image();
        let cropped_img = image::imageops::crop_imm(&img, x, y, width, height).to_image();
        Bitmap::from_rgba_image(&cropped_img)
    }
}

impl Clone for Bitmap {
    fn clone(&self) -> Self {
        Bitmap {
            width: self.width,
            height: self.height,
            pixels: self.pixels.clone(),
        }
    }
}