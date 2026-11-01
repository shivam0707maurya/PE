# PE – Android LAN Thermal Bill Printer App

Native Android application built in Kotlin + Jetpack Compose for **Pooja Enterprises** (पूजा इंटरप्राइजेज), Ahraura.

Directly prints item delivery bills to the **TVS RP 3210 Gold** thermal receipt printer over the local router network (LAN / Wi-Fi) on port 9100 without needing internet.

---

## 🚀 How to Build the APK on GitHub (Automated in 2 Minutes)

Since building Android APKs requires the Android SDK, this repository includes an automated **GitHub Actions CI/CD workflow** (`.github/workflows/build-apk.yml`) that automatically compiles and produces the installable `.apk` file whenever you push or upload this project to GitHub!

### Method 1: Upload via GitHub Website (No Git required)

1. Open [github.com](https://github.com) and log in.
2. Click **New repository** (name it `PE` or `pe-printer-app`), set to **Public** or **Private**, and click **Create repository**.
3. On the repository page, click **"uploading an existing file"** or drag-and-drop the files from this folder (`Desktop\PE`).
4. Commit the files.
5. Click on the **Actions** tab at the top of your GitHub repository.
6. You will see the **"Build Android APK"** workflow running automatically. It takes ~2 minutes.
7. Once finished:
   - Go to **Releases** on the right side of the repository page, OR
   - Click on the completed workflow run and look under **Artifacts** at the bottom.
8. Download **`PE-Shop-Printer.apk`** directly to your phone and install it!

---

### Method 2: Push via Git CLI

```bash
cd "C:\Users\shivam07maurya\Desktop\PE"
git init
git add .
git commit -m "Initial commit for PE printer app"
git branch -M main
git remote add origin https://github.com/<your-username>/PE.git
git push -u origin main
```
Once pushed, GitHub Actions immediately builds the `.apk` and publishes it under GitHub Releases!

---

## ✨ Features & Requirements Checklist

| Requirement | Status | Implementation Details |
|---|---|---|
| **App Identity** | ✅ Done | Name: **PE**. Logo: Three golden roofs outline with bold red "PE" matching shop visiting card. Android adaptive vector icons included. |
| **All UI in English** | ✅ Done | Buttons, labels, messages, settings in clean English. No Hinglish in UI. |
| **Multilingual Printing** | ✅ Done | Hindi (Devanagari), Gujarati and English render identically via Android Bitmap raster (`GS v 0`). |
| **Chunked ESC/POS** | ✅ Done | Sent in ~200 row chunks to port 9100, feed 4 lines (`ESC d 4`), auto-cut (`GS V 66 0`). |
| **Quantities** | ✅ Done | **Kg** always prints with 3 decimals (`25.000 Kg`, `250.200 Kg`). **Pcs** prints as whole number (`10 Pcs`). Item remembers default unit. |
| **First-Run Setup** | ✅ Done | Page 1: Import backup or Skip. Page 2: Business name, Header 2, Address, Printer IP (manual or Auto-scan), Paper size (58/80 mm). **No printer name asked.** |
| **Main New Bill Screen** | ✅ Done | Printer status pill (Idle/Printing/Busy/Paper cover/Offline), "Pending: N", Date/Time picker, Customer autocomplete, Items table with Pcs/Kg, WhatsApp toggle. |
| **Dynamic Visibility** | ✅ Done | Any item disabled in Settings > "What to print" is hidden from the New Bill screen and excluded from receipt. |
| **Print Preview** | ✅ Done | Exact visual preview matching the print. "Edit" and "Confirm and print". Switchable off in Settings. |
| **Queue & Never Disabled** | ✅ Done | Print button is never disabled. Bills queue automatically to Pending and print in order. Failed jobs allow Retry and Cancel. Persisted across app restarts. |
| **WhatsApp Sharing** | ✅ Done | Order: **Print first, then WhatsApp**. Only opens chooser when checked. Attaches generated PDF. Supports normal WhatsApp & WhatsApp Business. |
| **PDF Generation** | ✅ Done | Saved to `Documents/ShopPrinter/Bills/` named `Customer-Address-dd-MM-yyyy.pdf`. Scoped storage on Android 10+. Page width matches 80mm/58mm. |
| **Bill History** | ✅ Done | List, search by customer/date, Reprint, Send on WhatsApp, View PDF, Delete, Delete all. |
| **Auto Scan** | ✅ Done | Scans phone's Wi-Fi subnet (/24) for port 9100 in parallel with coroutines. 1-tap select. |
| **3 Built-in Styles** | ✅ Done | Simple, Table, Compact. JSON style import supported. |
| **Backup Export/Import** | ✅ Done | Full JSON backup file. Restore offers Merge or Replace. |

---

## 🖨️ Printer Connection Quick Guide (TVS RP 3210 Gold)

1. Connect the TVS RP 3210 Gold printer's **LAN port (RJ45)** to your shop Wi-Fi router with an Ethernet cable.
2. Turn on the printer power.
3. To check printer IP: Turn off printer, hold the **FEED** button, turn power on, release FEED after 2 seconds. The printer will print a test page showing its IP (e.g. `192.168.1.100` or DHCP assigned).
4. Connect your Android phone to the **same shop Wi-Fi**.
5. Open the **PE** app:
   - In Setup or Settings > Printer, enter the printer IP, or tap **Auto scan** to let the app find it automatically!
   - Tap **Test print** to verify!
