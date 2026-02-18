package com.magiconcall.domain.tool;

/**
 * Port interface for executable tools.
 * Each tool provides a name and an execute method.
 * Implementations live in the application layer (stubs) or infrastructure (real connectors).
 */
public interface Tool {

    String name();

    ToolResponse execute(ToolRequest request);
}
