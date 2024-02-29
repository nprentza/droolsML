### Drools-ML integration POC

[DROOLS-7572](https://issues.redhat.com/browse/DROOLS-7572)

Hybrid AI systems often leverage symbolic reasoning for high-level decision-making and machine learning for data-driven tasks, resulting in more robust and flexible systems.
This POC explores such a hybrid AI approach, the integration of drools rule-based engine with [emla](https://github.com/nprentza/emla),
a machine learning framework that provides functionality for learning rules from data. 

The POC currently demonstrates two basic use-cases, 
1. automatic DRL file (rules) creation from data,
2. DRL validation and automatic updates from data, to cover gaps or fix errors.

While the process of combining new with existing rules is not just about making sure that the rule does not already exist, for the purpose of this POC only this particular control was taken into account.

---

The **DataaccessApp** example.

The tabular dataset below holds a number of resource access requests from different users and the decision to these requests (allow, deny).

**Dataset**

| role        | experience | age | access |
| ----------- | ---------- | --- | ------ |
| admin       | senior     | 40  | allow  |
| admin       | senior     | 45  | allow  |
| admin       | senior     | 42  | allow  |
| contributor | senior     | 42  | allow  |
| contributor | junior     | 30  | deny   |
| contributor | junior     | 32  | deny   |
| contributor | senior     | 43  | allow  |
| guest       | junior     | 30  | deny   |
| guest       | senior     | 45  | deny   |


1. **DRL from data use-case**

- use `emla` to learn rules for the entire dataset, select one-rule for `allow` and one-rule for `deny` to initiate the DRL.
- the goal is to develop a DRL that supports the dataset with `100% coverage` and `0 errors`.
- `emla` returns the following rules per predictor (`role`,`experience`,`age`):
```
** rules for predictor 'role':

- IF role == admin THEN allow, (coverage=0.33, accuracy=1, assessment=0.33)
- IF role == contributor THEN allow, (coverage=0.44, accuracy=0.5, assessment=0.22)
- IF role == guest THEN deny, (coverage=0.22, accuracy=1, assessment=0.22)

** rules for predictor 'experience':

- IF experience == senior THEN allow, (coverage=0.67, accuracy=0.83, assessment=0.56)
- IF experience == junior THEN deny, (coverage=0.33, accuracy=1, assessment=0.33)

** rules for predictor 'age':

- IF (age > 36.0 AND age <= 45.0) THEN allow, (coverage=0.67, accuracy=0.83, assessment=0.56)
- IF age <= 36.0 THEN deny, (coverage=0.33, accuracy=1, assessment=0.33)
```
- the solution selects rules with the highest assessment, a metric that considers both the coverage and the accuracy of the rule.
- for `allow` the solution will select one of the rules with assessment 0.56 and for `deny` one of the rules with assessment 0.33.
- the DRL is initialized with one-rule for each {`allow`,`deny`} :

```
rule 'rule0' when
  $a: AgentDatapoint( age > 36.0 && age <= 45.0 ) 
then
 $a.setPrediction( 'allow' );
 update( $a ); 
end

rule 'rule1' when
  $a: AgentDatapoint( age <= 36.0 ) 
then
 $a.setPrediction( 'deny' );
 update( $a ); 
end
```
- the DRL is then is validated against the entire dataset: `Coverage=100%, Errors=1`
- `rule0` erroneously supports the last datapoint in the dataset:

  | role        | experience | age | access |
  | ----------- | ---------- | --- | ------ |
  | guest       | senior     | 45  | deny   |

- the process will try to learn a rule to fix this error.
- the learning/selection of rules is repeated but this time `emla` uses only the subset of data that `rule0` supports.
- the following rules are returned:
```
** rules for predictor 'role':

- IF role == admin THEN allow, (coverage=0.5, accuracy=1, assessment=0.5)
- IF role == contributor THEN allow, (coverage=0.33, accuracy=1, assessment=0.33)
- IF role == guest THEN deny, (coverage=0.17, accuracy=1, assessment=0.17)

** rules for predictor `experience`:

- IF experience == senior THEN allow, (coverage=1, accuracy=0.83, assessment=0.83) 

** rules for predictor `age`:

- IF age <= 45.0 THEN allow, (coverage=1, accuracy=0.83, assessment=0.83) 
```
- the learning process with select one-rule for `deny` to fix the error and update the DRL with `rule2`: 
```
rule 'rule2' when
  $a: AgentDatapoint( role ==  'guest'  ) 
then
 $a.setPrediction( 'deny' );
 update( $a ); 
end
```
- the DRL is then re-validated against the entire dataset: `Coverage=100%, Errors=0`.
- at this point the learning goal is reached and the process terminates.


2. **Validating & updating existing DRL use-case**

- use-case (2) shows how the `drools-verifier` module can be incorporated into the solution to ensure that a rule is missing before adding it to the DRL.
- for this integration we can currently use only numerical fields.
- the initial DRL contains the following rules:
```
rule 'rule0' when
  $a: AgentDatapoint( role ==  'admin'  ) 
then
 $a.setPrediction( 'allow' );
 update( $a ); 
end

rule 'rule1' when
  $a: AgentDatapoint( role ==  'guest'  ) 
then
 $a.setPrediction( 'deny' );
 update( $a ); 
end

rule 'rule2' when
  $a: AgentDatapoint( role ==  'contributor' , age > 39 )
then
 $a.setPrediction( 'allow' );
 update( $a ); 
end
```

- the DRL is validated against the entire dataset: `Coverage=77.78%, errors=2`
- two datapoints are not supported:

  | role        | experience | age | access |
  | ----------- | ---------- | --- | ------ |
  | contributor | junior     | 30  | deny   |
  | contributor | junior     | 32  | deny   |

- the process will use `emla` to find rules for these two datapoints.
- the following rules are returned:
```
** rules for predictor 'role':

- IF role == contributor THEN deny, (coverage=1, accuracy=1, assessment=1)

** rules for predictor `experience`:

- IF experience == junior THEN deny, (coverage=1, accuracy=1, assessment=1)

** rules for predictor `age`:

- IF (age >= 30.0 AND age <= 32.0) THEN deny, (coverage=1, accuracy=1, assessment=1)
```
- all rules have the highest assessment 1 and can be selected by the process to update the DRL.
- the process will select a numerical predictor to create `rule3` to showcase the integration potential with the `drools-verifier` module.

```
rule 'rule3' when
  $a: AgentDatapoint( age >= 30.0 && age <= 32.0 ) 
then
 $a.setPrediction( 'deny' );
 update( $a ); 
end
```
- the DRL is then re-validated against the entire dataset: `Coverage=100%, Errors=0`.
- at this point the learning goal is reached and the process terminates.
