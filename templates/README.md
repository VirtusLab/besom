# Templates

This directory contains the templates for `pulumi new`, 
which make it easy to quickly get started building new Pulumi projects written in Besom.

More information about Pulumi templates can be found in the 
[Pulumi templates GitHub repository](https://github.com/pulumi/templates).

## Adding a new template

1. Create a new directory for the template, e.g. `my-template`. 
By convention, hyphens are used to separate words.

2. Add template files in the new directory.

3. Request a review from the Besom team.

## Text replacement

The following special strings can be included in any template file; these will be replaced by the CLI when laying down the template files.

- `${PROJECT}` - The name of the project.
- `${DESCRIPTION}` - The description of the project.