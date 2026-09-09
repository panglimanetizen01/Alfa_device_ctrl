# Kebijakan Pin GitHub Actions

Semua `uses:` di `.github/workflows/*.yml` dan `.github/workflows/*.yaml` harus menggunakan SHA commit penuh 40 karakter.

Alasannya: GitHub mendokumentasikan pin SHA penuh sebagai cara immutable untuk mereferensikan action. Tag dapat dipindahkan atau dihapus, sedangkan SHA commit memberikan referensi yang tidak berubah.

Pembaruan action dilakukan dengan mengganti SHA secara eksplisit dan diverifikasi melalui CI.
