#include "../../native/touch_input.h"
#include <cassert>
#include <iostream>

using namespace touch::detail;
bool near(float a, float b) { return std::abs(a - b) < 0.00001f; }

int main() {
    // Floating: the first left finger owns movement, a spare finger cannot steal it or turn the camera.
    assert(RouteFreeTouch(false, false, true, false) == FreeTouchRole::Stick);
    assert(RouteFreeTouch(false, true, true, true) == FreeTouchRole::None);
    assert(RouteFreeTouch(false, false, false, true) == FreeTouchRole::Look);
    // Fixed: touches away from the stick still aim, including the unoccupied left side.
    assert(RouteFreeTouch(true, false, true, false) == FreeTouchRole::Look);
    assert(RouteFreeTouch(true, true, true, false) == FreeTouchRole::Stick);
    assert(RouteFreeTouch(true, true, true, true) == FreeTouchRole::None);
    // A customized stick can sit on the right; it still has only one owner.
    assert(RouteFreeTouch(true, true, false, false) == FreeTouchRole::Stick);
    assert(RouteFreeTouch(true, true, false, true) == FreeTouchRole::Look);

    // No drift at rest or inside the deadzone; full travel reaches full speed on every axis.
    for (float deadzone : {0.0f, 0.08f, 0.3f}) {
        auto rest = RemapStick(0, 0, deadzone);
        assert(rest.x == 0 && rest.y == 0);
        auto edge = RemapStick(deadzone, 0, deadzone);
        assert(edge.x == 0 && edge.y == 0);
        auto halfway = RemapStick(deadzone + (1.0f - deadzone) * 0.5f, 0, deadzone);
        assert(near(halfway.x, 0.5f));
        auto full = RemapStick(0, -1, deadzone);
        assert(near(full.y, -1) && full.x == 0);
        auto diagonal = RemapStick(2, 2, deadzone);
        assert(near(std::hypot(diagonal.x, diagonal.y), 1));
        assert(near(diagonal.x, diagonal.y));
    }
    // Button hit slop remains generous, but circles reject their bounding-box corners.
    assert(std::isfinite(TargetDistance(1.2f, 0, false)));
    assert(!std::isfinite(TargetDistance(1, 1, false)));
    assert(std::isfinite(TargetDistance(1.6f, 1, true)));
    assert(!std::isfinite(TargetDistance(1.61f, 0, true)));
    // At the small target's centre it wins against an overlapping large target regardless of array order.
    assert(TargetDistance(0, 0, false) < TargetDistance(0.3f, 0, false));
    std::cout << "Native touch routing, radial response and hit targets passed\n";
}
