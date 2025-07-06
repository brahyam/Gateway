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
            url: "https://github.com/brahyam/Gateway/releases/download/0.1.9-SNAPSHOT/Gateway.xcframework.zip",
            checksum: "2bb5ec1272d327fb0bbede4ca0cb2ec1e9bcea529190a22883472a9b43c74bdb"
        )
    ]
)
