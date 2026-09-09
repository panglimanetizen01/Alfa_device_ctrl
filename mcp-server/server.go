package main

import (
    "errors"
    "os"
    "runtime"
)

const alfaProjectRoot = "/sdcard/Alfa_device_ctrl_HOST/Alfa_device_ctrl"

func alfaDeviceInfo() string {
    return runtime.GOOS + "/" + runtime.GOARCH
}

func alfaProjectStatus(relativePath string) string {
    root, err := os.OpenRoot(alfaProjectRoot)
    if err != nil {
        return "error: " + err.Error()
    }
    defer root.Close()

    if relativePath == "" {
        relativePath = "."
    }

    info, err := root.Stat(relativePath)
    if err != nil {
        return "error: " + err.Error()
    }
    if !info.IsDir() {
        return "error: project path is not a directory"
    }
    return "ready"
}

func alfaListDirectory(relativePath string) string {
    root, err := os.OpenRoot(alfaProjectRoot)
    if err != nil {
        return "error: " + err.Error()
    }
    defer root.Close()

    dir, err := root.Open(relativePath)
    if err != nil {
        return "error: " + err.Error()
    }
    defer dir.Close()

    entries, err := dir.ReadDir(-1)
    if err != nil {
        return "error: " + err.Error()
    }

    result := ""
    for _, entry := range entries {
        if result != "" {
            result += "\n"
        }
        result += entry.Name()
    }
    return result
}

func alfaReadFile(relativePath string) string {
    data, err := readConfinedFile(relativePath)
    if err != nil {
        return "error: " + err.Error()
    }
    return string(data)
}

func readConfinedFile(relativePath string) ([]byte, error) {
    root, err := os.OpenRoot(alfaProjectRoot)
    if err != nil {
        return nil, err
    }
    defer root.Close()

    if relativePath == "" {
        return nil, errors.New("empty relative path")
    }

    return root.ReadFile(relativePath)
}
