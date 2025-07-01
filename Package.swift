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
            url: "https://github.com/brahyam/Gateway/releases/download/0.1.8/Gateway.xcframework.zip",
            checksum: "7bd2bc906adf07be64dbc6b77cb89f7ded986f3559a765ee424ff22dc2cda0ea"
        )
    ]
)
