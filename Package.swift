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
            url: "https://github.com/brahyam/gateway-kmp/releases/download/v0.1.0/Gateway.xcframework.zip",
            checksum: "4da4983901ef8b7596e89cd15ecb93e780fb91fbac7e7e98bbb20c26b0e284b1"
        )
    ]
)
