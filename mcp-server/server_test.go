package main

import (
    "os"
    "path/filepath"
    "strings"
    "testing"
)

func TestAlfaDeviceInfo(t *testing.T) {
    if got := alfaDeviceInfo(); got == "" {
        t.Fatal("device info is empty")
    }
}

func TestAlfaProjectStatus(t *testing.T) {
    root := t.TempDir()
    if got := alfaProjectStatusAt(root, ""); got != "ready" {
        t.Fatalf("root status = %q, want ready", got)
    }
    file := filepath.Join(root, "file.txt")
    if err := os.WriteFile(file, []byte("x"), 0o600); err != nil {
        t.Fatal(err)
    }
    if got := alfaProjectStatusAt(root, "file.txt"); got != "error: project path is not a directory" {
        t.Fatalf("file status = %q", got)
    }
}

func TestAlfaListDirectory(t *testing.T) {
    root := t.TempDir()
    if err := os.WriteFile(filepath.Join(root, "a.txt"), []byte("a"), 0o600); err != nil {
        t.Fatal(err)
    }
    got := alfaListDirectoryAt(root, ".")
    if !strings.Contains(got, "a.txt") {
        t.Fatalf("directory listing = %q", got)
    }
}

func TestAlfaReadFile(t *testing.T) {
    root := t.TempDir()
    if err := os.WriteFile(filepath.Join(root, "a.txt"), []byte("hello"), 0o600); err != nil {
        t.Fatal(err)
    }
    got, err := readConfinedFile(root, "a.txt")
    if err != nil {
        t.Fatal(err)
    }
    if string(got) != "hello" {
        t.Fatalf("content = %q", got)
    }
}

func TestAlfaPathConfinement(t *testing.T) {
    root := t.TempDir()
    outside := filepath.Join(filepath.Dir(root), "outside.txt")
    if err := os.WriteFile(outside, []byte("secret"), 0o600); err != nil {
        t.Fatal(err)
    }
    defer os.Remove(outside)

    if _, err := readConfinedFile(root, "../outside.txt"); err == nil {
        t.Fatal("path traversal unexpectedly succeeded")
    }
    if got := alfaListDirectoryAt(root, ".."); !strings.HasPrefix(got, "error:") {
        t.Fatalf("directory traversal unexpectedly succeeded: %q", got)
    }
    if got := alfaProjectStatusAt(root, "../"); !strings.HasPrefix(got, "error:") {
        t.Fatalf("status traversal unexpectedly succeeded: %q", got)
    }
}
