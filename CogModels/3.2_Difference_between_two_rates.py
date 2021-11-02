import arviz as az
import pymc3 as pm

## We're going to ignore some warnings that pymc3 can give
import warnings
warnings.simplefilter(action="ignore", category=FutureWarning)

## 3.2

## Suppose there are two different binary processes that have different
## probabilities of success. This model will find the difference in the
## those probabilities of success.

## We had 10 trials with each process
## Process 1 had 5 successes and process 2 had 7 successes
n_trials = 10
n_successes_1 = 5
n_successes_2 = 7

with pm.Model() as model:
    ## We will start with an uninformative Beta(1,1) prior
    ## which gives equal probability to all success chances
    ## between 0 and 1
    success_rate_1 = pm.Beta("success_rate_1", alpha=1, beta=1)
    success_rate_2 = pm.Beta("success_rate_2", alpha=1, beta=1)

    # Process 1 had `n_trials` with `n_successes_1` from a binomial process
    # binomail process with prior `success_rate_1`
    outcome_1 = pm.Binomial("outcome_1", n=n_trials, p=success_rate_1,
                            observed=n_successes_1)
    # Process 2 had `n_trials` with `n_successes_2` from a
    # binomail process with prior `success_rate_2`
    outcome_2 = pm.Binomial("outcome_2", n=n_trials, p=success_rate_2,
                            observed=n_successes_2)

    # The difference between the two processes is a deterministic value
    # that is the difference between their two success rates
    success_rate_difference = pm.Deterministic("success_rate_difference",
                                               success_rate_1 - success_rate_2)
    
    # Finally, we do our sampling and get the trace
    trace = pm.sample()

## Plot the posterior of the two success rates side by side to compare
az.plot_posterior(trace, var_names=["success_rate_1", "success_rate_2"], show=True)

## Plotting the difference between the success rates
## Remember, this is a belief distribution, so it shows that
## success_rate_1 is probably between -0.4 and +0.0 from success_rate_2
az.plot_dist(trace["success_rate_difference"], show=True)

## Finally, our summary statistics.
## We can see that that 94% of the probability is from
## -0.53 to 0.21, based on the hdi (high density interval)
## statistic.
print(az.summary(trace))

