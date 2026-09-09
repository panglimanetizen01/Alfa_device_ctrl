package main

import (
    "errors"
    "os"
    "path/filepath"
    "testing"
)

func TestReadConfinedFileRejectsOversizedFile(t *testing.T) {
    root := t.TempDir()
    path := filepath.Join(root, "large.bin")
    if err := os.WriteFile(path, make([]byte, maxMCPFileSize+1), 0600); err != nil {
        t.Fatal(err)
    }

    _, err := readConfinedFile(root, "large.bin")
    if !errors.Is(err, errMCPFileTooLarge) {
        t.Fatalf("expected %v, got %v", errMCPFileTooLarge, err)
    }
}

func TestReadConfinedFileAcceptsTextWithinLimit(t *testing.T) {
    root := t.TempDir()
    if err := os.WriteFile(filepath.Join(root, "ok.txt"), []byte("alfa"), 0600); err != nil {
        t.Fatal(err)
    }

    got, err := readConfinedFile(root, "ok.txt")
    if err != nil {
        t.Fatal(err)
    }
    if string(got) != "alfa" {
        t.Fatalf("got %q", got)
    }
}

func TestReadConfinedFileRejectsDirectory(t *testing.T) {
    root := t.TempDir()
    if err := os.Mkdir(filepath.Join(root, "dir"), 0700); err != nil {
        t.Fatal(err)
    }

    _, err := readConfinedFile(root, "dir")
    if err == nil {
        t.Fatal("expected directory rejection")
    }
}

func TestReadConfinedFileRejectsNonUTF8(t *testing.T) {
    root := t.TempDir()
    if err := os.WriteFile(filepath.Join(root, "binary.bin"), []byte{0xff, 0xfe, 0xfd}, 0600); err != nil {
        t.Fatal(err)
    }

    _, err := readConfinedFile(root, "binary.bin")
    if !errors.Is(err, errMCPFileNotText) {
        t.Fatalf("expected %v, got %v", errMCPFileNotText, err)
    }
}
