package main

import (
    "context"

    "github.com/modelcontextprotocol/go-sdk/mcp"
)

func newMCPServer() *mcp.Server {
    server := mcp.NewServer(
        &mcp.Implementation{
            Name:    "alfa-device-ctrl",
            Version: "0.1.0",
        },
        nil,
    )

    mcp.AddTool(server, &mcp.Tool{
        Name:        "alfa_device_info",
        Description: "Return basic Alfa Device Ctrl runtime information.",
    }, func(ctx context.Context, req *mcp.CallToolRequest, input struct{}) (*mcp.CallToolResult, struct {
        Device string `json:"device"`
    }, error) {
        return nil, struct {
            Device string `json:"device"`
        }{
            Device: alfaDeviceInfo(),
        }, nil
    })

    mcp.AddTool(server, &mcp.Tool{
        Name:        "alfa_project_status",
        Description: "Return the status of an Alfa project directory relative to the canonical Alfa workspace.",
    }, func(ctx context.Context, req *mcp.CallToolRequest, input struct {
        Path string `json:"path"`
    }) (*mcp.CallToolResult, struct {
        Status string `json:"status"`
    }, error) {
        return nil, struct {
            Status string `json:"status"`
        }{
            Status: alfaProjectStatus(input.Path),
        }, nil
    })

    mcp.AddTool(server, &mcp.Tool{
        Name:        "alfa_list_directory",
        Description: "List entries in an Alfa directory relative to the canonical Alfa workspace.",
    }, func(ctx context.Context, req *mcp.CallToolRequest, input struct {
        Path string `json:"path"`
    }) (*mcp.CallToolResult, struct {
        Entries string `json:"entries"`
    }, error) {
        return nil, struct {
            Entries string `json:"entries"`
        }{
            Entries: alfaListDirectory(input.Path),
        }, nil
    })

    mcp.AddTool(server, &mcp.Tool{
        Name:        "alfa_read_file",
        Description: "Read the contents of an Alfa file relative to the canonical Alfa workspace.",
    }, func(ctx context.Context, req *mcp.CallToolRequest, input struct {
        Path string `json:"path"`
    }) (*mcp.CallToolResult, struct {
        Content string `json:"content"`
    }, error) {
        return nil, struct {
            Content string `json:"content"`
        }{
            Content: alfaReadFile(input.Path),
        }, nil
    })

    return server
}
