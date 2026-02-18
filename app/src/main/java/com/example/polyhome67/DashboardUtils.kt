package com.example.polyhome67

object DashboardUtils {

    fun compute(devices: List<Device>): DashboardStats {
        val total = devices.size

        val active = devices.count { d ->
            (d.power != null && d.power > 0) || (d.opening != null && d.opening > 0)
        }

        val opened = devices.count { d -> d.opening != null && d.opening > 0 }
        val powered = devices.count { d -> d.power != null && d.power > 0 }

        val percent = if (total == 0) 0 else (active * 100) / total

        return DashboardStats(
            total = total,
            active = active,
            opened = opened,
            powered = powered,
            percentActive = percent
        )
    }
}
