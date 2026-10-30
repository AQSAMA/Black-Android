#pragma once

#include <algorithm>
#include <cmath>
#include <limits>

namespace touch::detail {

enum class FreeTouchRole { None, Stick, Look };

// Buttons and menus have already claimed their touches before movement/look routing.
inline FreeTouchRole RouteFreeTouch(bool fixed, bool inside_stick, bool on_left, bool stick_owned) {
    if ((fixed ? inside_stick : on_left) && !stick_owned) return FreeTouchRole::Stick;
    if (on_left || inside_stick) return FreeTouchRole::None;
    return FreeTouchRole::Look;
}

// Preserve sub-count motion instead of dropping every slow/high-refresh touch event.
struct LookAccumulator {
    float x = 0, y = 0;
    struct Counts { int x, y; };
    Counts add(float dx, float dy, float counts_per_pixel) {
        x += dx * counts_per_pixel;
        y += dy * counts_per_pixel;
        int ix = int(x), iy = int(y);
        x -= float(ix); y -= float(iy);
        return {ix, iy};
    }
};

struct StickVector { float x, y; };
inline StickVector RemapStick(float x, float y, float deadzone) {
    float length = std::hypot(x, y);
    deadzone = std::clamp(deadzone, 0.0f, 0.3f);
    if (length <= deadzone || length < 0.0001f) return {0, 0};
    float travel = std::clamp((length - deadzone) / (1.0f - deadzone), 0.0f, 1.0f);
    return {x * travel / length, y * travel / length};
}

// A normalized distance allows small targets to compete fairly with a nearby large fire button.
inline float TargetDistance(float dx, float dy, bool rectangle) {
    float distance = dx * dx + dy * dy;
    bool inside = rectangle ? std::abs(dx) <= 1.6f && std::abs(dy) <= 1.0f : distance <= 1.25f * 1.25f;
    return inside ? distance : std::numeric_limits<float>::infinity();
}

}  // namespace touch::detail
