// swift-tools-version:5.3
import PackageDescription

let package = Package(
    name: "Gateway",
    platforms: [
        .iOS(.v14),
    ],
    products: [
        .library(
            name: "Gateway",
            targets: ["Gateway"]
        )
    ],
    targets: [
        .binaryTarget(
            name: "Gateway",
            url: "https://github.com/brahyam/Gateway/releases/download/0.1.9/Gateway.xcframework.zip",
            checksum: "22e76711f327cfac156109dc667647eb7df00bbbebf31b5a03977d6cfc8e3080"
        )
    ]
)
