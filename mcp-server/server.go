package main

import (
    "errors"
    "io"
    "os"
    "runtime"
    "strings"
    "unicode/utf8"
)

const (
    alfaProjectRoot              = "/sdcard/Alfa_device_ctrl_HOST/Alfa_device_ctrl"
    maxMCPFileSize               = 1 << 20
    maxMCPDirectoryEntries       = 256
    maxMCPDirectoryResponseBytes = 32 << 10
)

var (
    errMCPFileTooLarge      = errors.New("file exceeds MCP read limit")
    errMCPFileNotText       = errors.New("file is not valid UTF-8 text")
    errMCPDirectoryTooLarge = errors.New("directory listing exceeds MCP response limit")
)

func alfaDeviceInfo() string {
    return runtime.GOOS + "/" + runtime.GOARCH
}

func alfaProjectStatus(relativePath string) string {
    return alfaProjectStatusAt(alfaProjectRoot, relativePath)
}

func alfaProjectStatusAt(rootPath, relativePath string) string {
    root, err := os.OpenRoot(rootPath)
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
    return alfaListDirectoryAt(alfaProjectRoot, relativePath)
}

func alfaListDirectoryAt(rootPath, relativePath string) string {
    root, err := os.OpenRoot(rootPath)
    if err != nil {
        return "error: " + err.Error()
    }
    defer root.Close()

    dir, err := root.Open(relativePath)
    if err != nil {
        return "error: " + err.Error()
    }
    defer dir.Close()

    entries, err := dir.ReadDir(maxMCPDirectoryEntries + 1)
    if err != nil && !errors.Is(err, io.EOF) {
        return "error: " + err.Error()
    }
    if len(entries) > maxMCPDirectoryEntries {
        return "error: " + errMCPDirectoryTooLarge.Error()
    }

    var result strings.Builder
    for i, entry := range entries {
        if i > 0 {
            result.WriteByte('\n')
        }
        result.WriteString(entry.Name())
        if result.Len() > maxMCPDirectoryResponseBytes {
            return "error: " + errMCPDirectoryTooLarge.Error()
        }
    }
    return result.String()
}

func alfaReadFile(relativePath string) string {
    data, err := readConfinedFile(alfaProjectRoot, relativePath)
    if err != nil {
        return "error: " + err.Error()
    }
    return string(data)
}

func readConfinedFile(rootPath, relativePath string) ([]byte, error) {
    root, err := os.OpenRoot(rootPath)
    if err != nil {
        return nil, err
    }
    defer root.Close()

    if relativePath == "" {
        return nil, errors.New("empty relative path")
    }

    file, err := root.Open(relativePath)
    if err != nil {
        return nil, err
    }
    defer file.Close()

    info, err := file.Stat()
    if err != nil {
        return nil, err
    }
    if !info.Mode().IsRegular() {
        return nil, errors.New("path is not a regular file")
    }
    if info.Size() > maxMCPFileSize {
        return nil, errMCPFileTooLarge
    }

    data, err := io.ReadAll(io.LimitReader(file, maxMCPFileSize+1))
    if err != nil {
        return nil, err
    }
    if len(data) > maxMCPFileSize {
        return nil, errMCPFileTooLarge
    }
    if !utf8.Valid(data) {
        return nil, errMCPFileNotText
    }
    return data, nil
}
