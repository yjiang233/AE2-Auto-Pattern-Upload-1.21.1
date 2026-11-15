# libs

编译期与开发运行期依赖，未纳入版本控制。

| 文件 | 用途 | 获取 |
| --- | --- | --- |
| `appliedenergistics2-19.2.19.jar` | AE2 1.21.1 NeoForge，必需 | [Modrinth ae2](https://modrinth.com/mod/ae2) 的 19.2.19 / 1.21.1 NeoForge |
| `guideme-21.1.22.jar` | AE2 19 的文档依赖，`runClient` / `runServer` 必需 | [Modrinth guideme](https://modrinth.com/mod/guideme) 的 21.1.22 |
| `jei-1.21.1-neoforge-19.70.0.469.jar` | JEI 19 API（编译期）与开发运行期验证 | [直链](https://cdn.modrinth.com/data/u6dRKJwZ/versions/1vC6z8wV/jei-1.21.1-neoforge-19.70.0.469.jar) / [Modrinth jei](https://modrinth.com/mod/jei) |

clone 后先按上表把 3 个 jar 放进本目录（或从已有的 `F:/MinecraftMods/libs` 复制），再执行 `./gradlew build`。发布用的 mod jar 不需要附带这些文件；JEI / EMI / REI 在运行期都是可选依赖。

Extended AE（`extended-ae` 1.21-2.2.35）仅用于核对装配矩阵实现了 `PatternContainer`，不作为工程依赖；开发时若想实测，把它的 jar 放进 `run/mods/` 即可。
