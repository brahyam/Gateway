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
            url:
                "https://github.com/brahyam/gateway-kmp/releases/download/0.1.2/Gateway.xcframework.zip",
            checksum: "f7ba1efe2dd03f4de5e2ae12e068b14a1b6e766933be1a8820e2fbba11e0b684"
        )
    ]
)
