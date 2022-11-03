/*
 * Copyright (C) 2022 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

#include <android-base/logging.h>
#include <android-base/properties.h>

#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <stdio.h>
#include <stdlib.h>
#include <sys/sysinfo.h>
#include <sys/system_properties.h>
#include <sys/_system_properties.h>

#include "property_service.h"
#include "vendor_init.h"

using android::base::GetProperty;
using std::string;

std::vector<std::string> ro_props_default_source_order = {
"",
"odm.",
"product.",
"system.",
"system_ext.",
"vendor.",
"vendor_dlkm."
};

/*
 * SetProperty does not allow updating read only properties and as a result
 * does not work for our use case. Write "property_override" to do practically
 * the same thing as "SetProperty" without this restriction.
 */

void property_override(char const prop[], char const value[], bool add = true) {
  prop_info *pi;

  pi = (prop_info*) __system_property_find(prop);
  if (pi)
    __system_property_update(pi, value, strlen(value));
  else if (add)
    __system_property_add(prop, strlen(prop), value, strlen(value));
}

void set_ro_build_prop(const std::string &prop, const std::string &value, bool product = true) {
  string prop_name;
  
  for (const auto &source : ro_props_default_source_order) {
    if (product)
      prop_name = "ro.product." + source + prop;
    else
      prop_name = "ro." + source + "build." + prop;
    
    property_override(prop_name.c_str(), value.c_str());
   }
}

void vendor_load_properties() {
  string model;
  string device;
  string name;

 /*
  * Only for read-only properties. Properties that can be wrote to more
  * than once should be set in a typical init script (e.g. init.oplus.hw.rc)
  * after the original property has been set.
  */
  
  auto prjname = std::stoi(GetProperty("ro.boot.prjname", "0"));
  auto rf_version = std::stoi(GetProperty("ro.boot.rf_version", "0"));

  switch(prjname){
    /* OnePlus 9 */
    case 19825:
      device = "OnePlus9";
      /* Bluetooth */
      property_override("bluetooth.device.default_name", "OnePlus 9");
      /* Graphics */
      property_override("ro.surface_flinger.set_idle_timer_ms", "4000");
      property_override("ro.surface_flinger.set_touch_timer_ms", "4000");
      /* USB */
      property_override("vendor.usb.product_string", "OnePlus 9");
      switch (rf_version){
        /* China */
        case 11:
          name = "OnePlus9_CN";
          model = "LE2110";
          break;
        /* India */
        case 13:
          name = "OnePlus9_IN";
          model = "LE2111";
          break;
        /* Europe */
        case 21:
          name = "OnePlus9_EU";
          model = "LE2113";
          break;
        /* Global / US Unlocked */
        case 22:
          name = "OnePlus9_NA";
          model = "LE2115";
          break;
        /* Generic */
        default:
          name = "OnePlus9_NA";
          model = "LE2115";
          break;
      }
      break;
    /* OnePlus 9 T-Mobile */
    case 20854:
      device = "OnePlus9";
      /* Bluetooth */
      property_override("bluetooth.device.default_name", "OnePlus 9");
      /* Graphics */
      property_override("ro.surface_flinger.set_idle_timer_ms", "4000");
      property_override("ro.surface_flinger.set_touch_timer_ms", "4000");
      /* USB */
      property_override("vendor.usb.product_string", "OnePlus 9");
      switch (rf_version){
        /* T-Mobile */
        case 12:
          name = "OnePlus9_TMO";
          model = "LE2117";
          break;
        /* Generic */
        default:
          name = "OnePlus9_NA";
          model = "LE2115";
          break;
      }
      break;
    /* OnePlus 9 Pro */
    case 19815:
      device = "OnePlus9Pro";
      /* Bluetooth */
      property_override("bluetooth.device.default_name", "OnePlus 9 Pro");
      /* Graphics */
      property_override("ro.surface_flinger.set_idle_timer_ms", "250");
      property_override("ro.surface_flinger.set_touch_timer_ms", "300");
      /* Resolution switch */
      property_override("ro.display.resolution.custom", "true");
      /* USB */
      property_override("vendor.usb.product_string", "OnePlus 9 Pro");
      switch (rf_version){
        /* China */
        case 11:
          name = "OnePlus9Pro_CN";
          model = "LE2120";
          break;
        /* India */
        case 13:
          name = "OnePlus9Pro_IN";
          model = "LE2121";
          break;
        /* Europe */
        case 21:
          name = "OnePlus9Pro_EU";
          model = "LE2123";
          break;
        /* Global / US Unlocked */
        case 22:
          name = "OnePlus9Pro_NA";
          model = "LE2125";
          break;
        /* Generic */
        default:
          name = "OnePlus9Pro_IN";
          model = "LE2125";
          break;
      }
      break;
    /* OnePlus 9 Pro T-Mobile */
    case 133210:
      device = "OnePlus9Pro";
      /* Bluetooth */
      property_override("bluetooth.device.default_name", "OnePlus 9 Pro");
      /* Graphics */
      property_override("ro.surface_flinger.set_idle_timer_ms", "250");
      property_override("ro.surface_flinger.set_touch_timer_ms", "300");
      /* Resolution switch */
      property_override("ro.display.resolution.custom", "true");
      /* USB */
      property_override("vendor.usb.product_string", "OnePlus 9 Pro");
      switch (rf_version){
        /* T-Mobile */
        case 12:
          name = "OnePlus9Pro_TMO";
          model = "LE2127";
          break;
        /* Generic */
        default:
          name = "OnePlus9Pro_NA";
          model = "LE2125";
          break;
      }
      break;
  }
  set_ro_build_prop("device", device);
  set_ro_build_prop("model", model);
  set_ro_build_prop("name", name);
  set_ro_build_prop("product", model, false);
}
