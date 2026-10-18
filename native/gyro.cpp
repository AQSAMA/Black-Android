// Aiming by turning the phone, as mobile shooters have it: the gyroscope's turn rates become mouse counts, which the
// game's mouse look (src/game/mouse.cpp) turns the view by. Off, always on, or only while firing; the mode and the
// sensitivity are set in the game's options menu and kept in android.toml beside the game's settings.
#include <SDL3/SDL.h>

#include <algorithm>
#include <atomic>
#include <cmath>
#include <fstream>

#include <toml++/toml.hpp>

#include "app/app.h"
#include "core/log.h"
#include "host.h"

namespace gyro {

namespace {

constexpr float kDegreesPerCount = 0.05f;  // src/game/mouse.cpp's kDegrees
constexpr float kDeadZone = 0.015f;        // radians a second: a hand held still
constexpr const char* kNames[kModes] = {"OFF", "ON", "FIRE"};

std::atomic<uint32_t> g_mode{kOff};
float g_sensitivity = 1.5f;  // view degrees per degree the phone turns
bool g_loaded = false;
SDL_Sensor* g_sensor = nullptr;
uint64_t g_last_ns = 0;

std::filesystem::path File() { return app::ExeDir() / "android.toml"; }

void Load() {
    g_loaded = true;
    try {
        toml::table t = toml::parse_file(File().string());
        g_mode = std::min<uint32_t>(t["gyro"].value_or(uint32_t(kOff)), kModes - 1);
        g_sensitivity = float(t["gyro_sensitivity"].value_or(double(g_sensitivity)));
    } catch (...) {
    }
}

void Save() {
    toml::table t{{"gyro", int64_t(g_mode.load())}, {"gyro_sensitivity", double(g_sensitivity)}};
    std::ofstream(File()) << t << "\n";
}

SDL_SensorID FindGyro() {
    int n = 0;
    SDL_SensorID found = 0;
    if (SDL_SensorID* ids = SDL_GetSensors(&n)) {
        for (int i = 0; i < n && !found; i++)
            if (SDL_GetSensorTypeForID(ids[i]) == SDL_SENSOR_GYRO) found = ids[i];
        SDL_free(ids);
    }
    return found;
}

}  // namespace

uint32_t CurrentMode() {
    if (!g_loaded) Load();
    return g_mode;
}

// the game's options menu (src/common/menu.cpp) sets these; saved at once
void SetMode(uint32_t mode) {
    if (!g_loaded) Load();
    g_mode = mode % kModes;
    Save();
}

float Sensitivity() {
    if (!g_loaded) Load();
    return g_sensitivity;
}

void SetSensitivity(float s) {
    if (!g_loaded) Load();
    g_sensitivity = std::clamp(s, 0.1f, 5.0f);
    Save();
}

const char* ModeName() { return kNames[g_mode % kModes]; }

void NextMode() {
    g_mode = (g_mode + 1) % kModes;
    LOGI("gyro: %s", ModeName());
    Save();
}

void Pump() {
    if (!g_loaded) Load();
    bool want = g_mode != kOff && host::Foreground();
    if (want == (g_sensor != nullptr)) return;
    if (!want) {
        SDL_CloseSensor(g_sensor);
        g_sensor = nullptr;
        return;
    }
    if (!SDL_WasInit(SDL_INIT_SENSOR) && !SDL_InitSubSystem(SDL_INIT_SENSOR)) return;
    SDL_SensorID id = FindGyro();
    g_sensor = id ? SDL_OpenSensor(id) : nullptr;
    g_last_ns = 0;
    if (!g_sensor) {
        LOGW("gyro: no gyroscope on this device");
        g_mode = kOff;
    }
}

// SDL's gyro reads in the phone's natural (portrait) axes, radians a second: x right, y up, z out of the screen.
// Held in landscape, the turn about the world's up axis is the turn about the phone's x axis and the tilt about the
// world's right axis is the turn about its y axis, signed by which side is up
void OnEvent(const SDL_Event& e) {
    if (e.type != SDL_EVENT_SENSOR_UPDATE || !g_sensor || e.sensor.which != SDL_GetSensorID(g_sensor)) return;
    uint64_t now = e.sensor.sensor_timestamp ? e.sensor.sensor_timestamp : e.sensor.timestamp;
    float dt = g_last_ns && now > g_last_ns ? float(now - g_last_ns) * 1e-9f : 0;
    g_last_ns = now;
    if (dt <= 0 || dt > 0.1f) return;
    bool firing = g_mode == kFiring ? touch::Firing() : true;
    if (!firing || !touch::LookActive()) return;
    float wx = e.sensor.data[0], wy = e.sensor.data[1];
    if (std::fabs(wx) < kDeadZone) wx = 0;
    if (std::fabs(wy) < kDeadZone) wy = 0;
    if (!wx && !wy) return;
    SDL_Window* w = host::Window();
    bool flipped = w && SDL_GetCurrentDisplayOrientation(SDL_GetDisplayForWindow(w)) == SDL_ORIENTATION_LANDSCAPE_FLIPPED;
    float yaw = flipped ? -wx : wx;    // positive: the phone turns left
    float pitch = flipped ? wy : -wy;  // positive: its top tilts back, the view looks up
    float k = g_sensitivity * dt * (180.0f / 3.14159265f) / kDegreesPerCount;
    touch::AddLook(-yaw * k, -pitch * k);
}

}  // namespace gyro
