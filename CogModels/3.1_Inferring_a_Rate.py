import arviz as az
import pymc3 as pm

## We're going to ignore some warnings that pymc3 can give
import warnings
warnings.simplefilter(action="ignore", category=FutureWarning)

## 3.1

## In this model, there is a single underlying rate for a binary process

# We had 5 successes out of 10 trials
n_successes = 5
n_trials = 10

with pm.Model() as model1:
    # Our prior for the success of the process
    # is the Beta(1,1) distribution, which gives
    # the same probability to all numbers from 0 to 1
    success_rate = pm.Beta("success_rate", alpha=1, beta=1)
    
    # The process is binary (Bernoulli). As discussed,
    # a Bernoulli process that happens several times
    # is the Binomial process.
    # In this case, we had `n_trials` with `n_successes`.
    # Our prior for the success rate comes from `success_rate` above
    outcome = pm.Binomial("outcome", n=n_trials, p=success_rate,
                          observed=n_successes)

    # Finally, we perform the Bayesian inference step
    # The output of sampling includes the entire "trace" of the sample,
    # which has plenty of information for trouble-shooting as well as our
    # posterior prediction
    trace = pm.sample()

## Plot our success_rate posterior distribution
az.plot_dist(trace["success_rate"], show=True)

## Finally, print details like mean, standard deviation
## and confidence intervals to the terminal
print(az.summary(trace, var_names=["success_rate"], round_to=3))
