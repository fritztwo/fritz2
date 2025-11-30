# Website

The project website is hosted via GitHub pages. The snapshot- and release versions are deployed in different 
repositories, though:

1) The snapshot version ([next.fritz2.dev](https://next.fritz2.dev)) is deployed to the `gh_pages` branch of _this_
repository. It is generated and deployed every time the `master` branch is built.
2) The release version is deployed to a separate repository: [fritztwo/website](https://github.com/fritztwo/website).
This is due to the fact each repository can only host one GitHub pages website.

## Environment

| Secret Name             | Description                                                                                              |
|-------------------------|----------------------------------------------------------------------------------------------------------|
| `RELEASE_WEBSITE_TOKEN` | Access token with write permissions to the [fritztwo/website](https://github.com/fritztwo/website) repo  |

> [!NOTE]
> The above access token is only needed to publish the _release_ version of the website as it isn't hosted in the same
> repository as the pipeline is running in.
> In order to publish the _snapshot_ version, not token generation is needed as there is an automatically generated
> token available in the pipeline's environment out of the box (automatically provided by GitHub).