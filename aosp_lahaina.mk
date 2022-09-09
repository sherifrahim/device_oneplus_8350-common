#
# Copyright (C) 2021-2022 The Nameless-AOSP Project
#
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from OnePlus lahaina device
$(call inherit-product, device/oneplus/lahaina/device.mk)

# Inherit some common Nameless-AOSP stuff.
$(call inherit-product, vendor/aosp/config/common_full_phone.mk)

# Boot Animation
TARGET_BOOT_ANIMATION_RES := 1080

# Official
CUSTOM_BUILD_TYPE := Official

# Device identifier. This must come after all inclusions.
PRODUCT_NAME := aosp_lahaina
PRODUCT_DEVICE := lahaina
PRODUCT_MANUFACTURER := OnePlus
PRODUCT_BRAND := OnePlus
PRODUCT_MODEL := lahaina

PRODUCT_GMS_CLIENTID_BASE := android-oneplus
