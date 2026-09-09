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
    if strings.HasPrefix(got, "error: ") {
        return
    }
    if len(strings.Split(got, "\n")) > maxMCPDirectoryEntries {
        t.Fatal("listing exceeded entry limit")
    }
}

func TestAlfaListDirectoryRejectsOversizedResponse(t *testing.T) {
    root := t.TempDir()
    for i := 0; i < 4; i++ {
        name := strings.Repeat("x", maxMCPDirectoryResponseBytes/2)
        path := filepath.Join(root, fmt.Sprintf("%s-%d", name, i))
        if err := os.WriteFile(path, []byte("x"), 0600); err != nil {
            t.Fatal(err)
        }
    }

    got := alfaListDirectoryAt(root, ".")
    if !strings.HasPrefix(got, "error: ") {
        t.Fatalf("expected response-size rejection, got %d bytes", len(got))
    }
}
