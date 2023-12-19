### Drools-ML integration POC

[DROOLS-7572](https://issues.redhat.com/browse/DROOLS-7572)

The idea is to combine the domain expert's knowledge, that is available in a symbolic form (DRL rules), with rules that can be derived from data, using a machine learning algorithm, 
to build a more comprehensive Drools application. 

The POC currently includes a very basic example that shows,
- how a DRL can be validated for its coverage on a given set of data, 
- how the [emla](https://github.com/nprentza/emla) libray can be used to discover additional rules for cases not covered by the DRL (if any), and,
- how the gap-analysis provided by the `drools-verifier` module can be incorporated into the solution to ensure that a rule is missing before adding it to the DRL.

While the process of combining new with existing rules is not just about making sure that the rule does not already exist, for the purpose of this POC only this particular control was taken into account.

**The `DroolsAgentApp` example:**
- The initial DRL contains the following rules:
```
rule 'AllowAdmin' when
	$a: Agent( role == "admin" )
then
    $a.setGrantAccess( true );
	allow.add( $a.getId() );
end
rule 'DenyGuest' when
	$a: Agent( role == "guest" )
then
    $a.setGrantAccess( false );
	deny.add( $a.getId() );
end
rule 'DenyChildren' when
	$a: Agent( age < 19 )
then
    $a.setGrantAccess( false );
    deny.add( $a.getId() );
end
```
- A csv file contains 7 `Agent` requests: 2 requests from `admin` agents, 3 requests from `guest` and 2 requests from `contributor` agents.
- Data from the csv file are used to insert `Agent` facts into the working memory.
- To validate the initial DRL for data coverage we call `fireAllRules()` and then check which facts have not been processed.

**(a) identify the cases not covered:**
- The validation results showed that the initial DRL does not support the 2 `contributor` agent requests:

| role        | experience | age | access |
| ----------- | ---------- | --- | ------ | 
| contributor | junior     | 32  | allow  |
| contributor | senior     | 42  | allow  |

**(b) use `emla` to derive rules for these cases:**
- Rules derived from the `OneR` algorithm:
```
(1) IF role == contributor THEN allow. (Coverage= 100%, error=0%)
(2) IF experience == junior THEN allow. (Coverage= 50%, error=0%)
(3) IF experience == senior THEN allow. (Coverage= 50%, error=0%)
(4) IF (age >= 32.0 AND age <= 42.0) then allow. (Coverage= 100%, error=0%)
```
**(c) revise the initial DRL:**
- Rules with coverage=100% and error=0% are preferred.
- `drools-verifier` gap-analysis results showed that there exist a gap for the field `age` and the range of values `>=19`.
- The DRL is revised with the addition of rule (4) for which we know that a gap exists for `age >= 32 and age <=42`: 
```
rule 'allow_age32_age42' when
    $a: Agent( role == AgentRole.ADMIN )
then
    $a.setGrantAccess( true );
    allow.add( $a.getId() );
end
```

**(d) repeat the validation process:**
- Repeating the process, the revised DRL covered the dataset 100%.

