package com.oddlabs.converter;


public record Bone(String name, byte index, Bone[] children) {
    public Bone(String name, byte index, Bone[] children) {
        this.name = name;
        this.children = children;
        this.index = index;
    }


}
