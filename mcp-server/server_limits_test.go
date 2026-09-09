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
