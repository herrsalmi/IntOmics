<!-- badges: start -->
[![Generic badge](https://img.shields.io/badge/version-0.9--alpha.1-green)](https://shields.io/)
[![License: GPL v3](https://img.shields.io/badge/license-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
<!--badges: end -->
## Multi omics data integration tool
IntOmics is a tool for integrating proteomics and transcriptomics human data in order to uncover cell crosstalk mechanisms.


## Usage
```
java -jar intOmics.jar -p <file> -a <file> -g <file> [options]*
```

## Parameters

| Argument          | Description                                           |
|:------------------|:------------------------------------------------------|
| -p \<file>        | Text file containing secreted proteins                |
| -a \<file>        | Text file containing all expressed genes              |
| -g \<file>        | Text file containing differentially expressed genes   |
| -f \<string>      | Output format: TSV or FWF                             |
| -db \<string>     | Pathway database: [KEGG, WikiPathways, Reactome]      |
| -s \<int>         | Minimum score for PPI (range from 0 to 999)           |
| -fc \<double>     | Fold change cutoff                                    |
| -pv \<double>     | P-value cutoff                                        |
| -t \<int>         | Number of threads to use                              |
| -d \<string>      | Custom separator for input files                      |
| --no-cached-sets  | Pull an up to date list of pathways                   |
| --no-cached-ppi   | Disable usage of cached PPI data                      |
| -h                | Print the help screen                                 |


## Input files
Input files should be in CSV format and can have a header line starting with `#`. The default column separator is `;`, but a different one can be specified using the argument `-d`.

##### Secreted proteins
Text file containing protein names or corresponding Entrez gene id, each one on a separate line.
##### Expressed genes
Text file containing symbols for all expressed genes, each one on a separate line. This list is used to infer membrane protein-coding genes.
##### Differential expression testing results
Text file in CSV format with three columns: `gene name`, `p value` and `fold change`. 


## Outputs
There are two main output files:
* A text file either in TSV or FWF format containing:
    * **Protein**: secreted proteins symbol.
    * **Protein description**: full name of the protein.
    * **Gene**: symbol corresponding to membrane protein-coding gene.
    * **Gene description**: full name of the gene.
    * **I score**: interaction score between the protein and the receptor.
    * **Pathways**: list of pathways with and enrichment score and a p-value.
* An HTML file representing the network of interaction between secreted proteins and receptors.
    
## Gene set enrichment analysis
The GSEA implemented in this tool is slightly different than the on proposed by Subramanian et al. (2005).

Gene sets represent pathways from either `KEGG`, `Wikipathways` or `Reactome`. `Wikipathways` is chosen by default if argument `-db` is not specified. 
This tool has prebuilt sets for `Wikipathways` and `KEGG`, but an up to date version can be rebuilt by using the argument `--no-cached-sets`.
Note that if no new pathways exist, the prebuilt version will be used.
This argument though has no effect when using `Reactome` as no prebuilt sets are available, and the online service is always queried.
 
## Protein-protein interactions
Protein-protein interactions data from StringDB is used to establish a link between secreted proteins and surface receptors.
Interaction scores are between 0 and 999 and they do not indicate the strength or the specificity of the interaction.
Instead, they are indicators of confidence.

| score               | confidence              |
|:--------------------|:------------------------|
| x >= 900            | highest confidence      |
| x >= 700            | high confidence         |
| x >= 400            | medium confidence       |
| x >= 150            | low confidence          |

A cached network of PPI is used when the interaction score threshold is greater than 700.
You can override this behavior by using the argument `--no-cached-ppi`.







