package main

import (
    "fmt"
    "os"
    "path/filepath"
    "strings"
    "testing"
)

func TestAlfaListDirectoryLimitsEntries(t *testing.T) {
    root := t.TempDir()
    for i := 0; i < maxMCPDirectoryEntries+5; i++ {
        path := filepath.Join(root, fmt.Sprintf("entry-%06d", i))
        if err := os.WriteFile(path, []byte("x"), 0600); err != nil {
            t.Fatal(err)
        }
    }

    got := alfaListDirectoryAt(root, ".")
    want := "error: directory listing exceeds MCP entry limit"
    if got != want {
        t.Fatalf("expected %q, got %q", want, got)
    }
}

func TestAlfaListDirectoryRejectsOversizedResponse(t *testing.T) {
    root := t.TempDir()
    for i := 0; i < maxMCPDirectoryEntries; i++ {
        name := strings.Repeat("x", 200) + fmt.Sprintf("%03d", i)
        path := filepath.Join(root, name)
        if err := os.WriteFile(path, []byte("x"), 0600); err != nil {
            t.Fatal(err)
        }
    }

    got := alfaListDirectoryAt(root, ".")
    want := "error: directory listing exceeds MCP response limit"
    if got != want {
        t.Fatalf("expected %q, got %q", want, got)
    }
}
