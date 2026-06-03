# 🌌 Aero TPA

**Aero TPA** brings a premium, ultra-clean Graphical User Interface (GUI) to your server's teleportation system—heavily inspired by modern, high-end SMP layouts. 

Give your players an intuitive, click-and-go experience that fits perfectly into any modern Minecraft server network.

---

## ✨ Features

* **📺 GUI-Driven:** Manage incoming and outgoing requests inside a beautifully designed chest GUI.
* **🤖 Smart Auto-Accept:** Players can toggle `/tpauto` to automatically accept incoming requests from their friends or community.
* **⚡ GUI Auto-Confirm:** Skip the extra clicks! Toggle automatic confirmation to completely bypass the GUI confirmation screens for instant handling.
* **🚫 Request Toggling:** Block distractions instantly by toggling all incoming teleport requests on or off when you need some peace and quiet.
* **🔄 Dual Request Support:** Full support for both standard Teleport Ask (`/tpa`) and Teleport Ask Here (`/tpahere`).
* **🎨 Highly Customizable:** Easily tweak the GUI layout, item icons, titles, and color schemes to match your server's branding.

---

## 🛠️ Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/tpa <player>` | Opens a GUI to confirm the teleport request | `aerotpa.use.tpa` |
| `/tpahere <player>` | Opens a GUI to confirm and request a player to teleport to you | `aerotpa.use.tpahere` |
| `/tpaccept [player]` | Accept a request | `aerotpa.use.accept` |
| `/tpadeny [player]` | Deny a request | `aerotpa.use.deny` |
| `/tpauto` | Toggles automatic teleport acceptance on/off | `aerotpa.use.auto` |
| `/tpaconfirm` | Toggles automatic confirmation of GUI prompts on/off | `aerotpa.use.autoconfirm` |
| `/tpatoggle` | Toggles all incoming teleport requests on/off | `aerotpa.use.toggle` |
| `/tpaui` | Open your pending requests dashboard | `aerotpa.use.gui` |
| `/tpareload` | Reload the configuration file | `aerotpa.admin` |

---

## ⚙️ Configuration

The plugin generates a simple, easy-to-read `config.yml` on first boot. You can customize:
* Request expiration timeouts (in seconds).
* Teleport warmup/cooldown delays (with optional movement cancellation).
* Every single text string, item material, confirmation icon, and lore line used in the interface.

---

## 📥 Download
You can download the latest compiled version of AeroTPA from the [Releases](https://github.com/MahanYTT/AeroTPA/releases) page on GitHub.
